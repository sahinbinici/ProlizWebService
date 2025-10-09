# Push Notification Sistemi - Mimari Dokümantasyon

## 🏗️ Sistem Mimarisi

### Genel Bakış

Bu proje **hibrit bir veri yönetimi** kullanır:
- **SOAP Web Service'den** gelen veriler **memory'de cache'lenir** (veritabanına yazılmaz)
- **Push notification** verileri **veritabanına yazılır**

```
┌─────────────────────────────────────────────────────────────┐
│                    SOAP Web Service                         │
│              (Öğrenci Bilgi Sistemi)                        │
│   - Öğrenci Listesi                                         │
│   - Ders Listesi                                            │
│   - Öğretim Elemanı Listesi                                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ XML/SOAP
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              OgrenciWebServiceClient                        │
│              (SOAP Client)                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Parse XML
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              DataCacheService                               │
│              (Memory Cache - ConcurrentHashMap)             │
│   ┌─────────────────────────────────────────────────┐      │
│   │ allDersler: List<Ders>                          │      │
│   │ allOgretimElemanlari: List<OgretimElemani>      │      │
│   │ dersOgrencileriMap: Map<DersID, List<Ogrenci>>  │      │
│   │ ogrenciDerslerIndex: Map<OgrNo, List<Ders>>     │      │
│   └─────────────────────────────────────────────────┘      │
│   ⚠️  VERİTABANINA YAZILMAZ - SADECE MEMORY'DE              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Cache'den Oku
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              NotificationService                            │
│   1. Cache'den öğretim elemanının derslerini al            │
│   2. Cache'den ders öğrencilerini al                       │
│   3. DB'den öğrenci token'larını al                        │
│   4. Token'larla bildirim gönder                           │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌──────────────────┐    ┌──────────────────────┐
│   Database       │    │  Expo Push Service   │
│   (MariaDB)      │    │  (External API)      │
│                  │    │                      │
│ - Token'lar      │    │ - Bildirim Gönder    │
│ - History        │    │ - Push Delivery      │
└──────────────────┘    └──────────────────────┘
```

## 📊 Veri Kaynakları

### 1. SOAP Web Service (Memory Cache)

**Veri Akışı:**
```
SOAP Service → XML Response → XmlParser → POJO → DataCacheService (Memory)
```

**Cache'lenen Veriler:**
- `List<Ders>` - Tüm dersler
- `List<OgretimElemani>` - Tüm öğretim elemanları
- `Map<String, List<Ogrenci>>` - Ders-Öğrenci ilişkisi
- `Map<String, List<Ders>>` - Öğrenci-Ders index'i

**Özellikler:**
- ✅ Progressive loading (arka planda yükleme)
- ✅ ConcurrentHashMap (thread-safe)
- ✅ Index'ler ile hızlı arama
- ✅ Periyodik refresh
- ❌ Veritabanına yazılmaz

**Örnek Kullanım:**
```java
// Cache'den ders bilgisi al
Ders ders = cacheService.getDersByHarId("2838793");

// Cache'den öğrenci listesi al
List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId("2838793");

// Cache'den öğretim elemanı al
OgretimElemani ogretimElemani = cacheService.getOgretimElemaniBySicil("12345");
```

### 2. Database (MariaDB)

**Sadece Notification Verileri:**
```sql
-- Token'lar (cihaz kayıtları)
notification_tokens
  - id
  - token (Expo push token)
  - user_id (öğrenci no veya sicil no)
  - user_type (STUDENT / ACADEMIC)
  - platform, device_id, os_version
  - created_at, updated_at

-- Bildirim geçmişi
notification_history
  - id
  - academic_id (gönderen)
  - lesson_id (hangi ders)
  - title, body
  - recipient_type (ALL / CLASS / INDIVIDUAL)
  - class_id
  - recipient_count, sent_count, failed_count
  - created_at
```

**Neden Sadece Notification Verileri?**
- Öğrenci/ders verileri zaten SOAP'tan geliyor
- Gerçek zamanlı veri için SOAP tek kaynak
- Notification token'ları cihaza özel (DB'de saklanmalı)
- Bildirim geçmişi raporlama için gerekli

## 🔄 Bildirim Gönderme Akışı

### Adım Adım İşlem

```
1. Akademik Personel → Mobil Uygulama
   ↓
2. Ders Seçimi
   ↓
3. NotificationController.getAcademicLessons(sicilNo)
   ↓
4. NotificationService:
   a. Cache'den öğretim elemanı bul (sicil → TC)
   b. Cache'den dersleri filtrele (TC ile)
   c. Her ders için cache'den öğrenci sayısı
   ↓
5. Alıcı Seçimi (Tümü / Sınıf / Bireysel)
   ↓
6. NotificationController.sendBulkNotification()
   ↓
7. NotificationService:
   a. Cache'den öğrenci listesi al
   b. DB'den öğrenci token'larını al
   c. Token'ları Expo Push Service'e gönder
   d. Sonucu DB'ye kaydet (history)
   ↓
8. Expo Push Service → Cihazlara Bildirim
```

### Kod Örneği

```java
// 1. Akademisyenin derslerini al (CACHE'den)
List<LessonInfo> lessons = notificationService.getAcademicLessons("12345");
// → DataCacheService.getOgretimElemaniBySicil()
// → DataCacheService.getAllDersler() + filter
// → DataCacheService.getOgrencilerByDersHarId()

// 2. Ders öğrencilerini al (CACHE'den)
List<StudentInfo> students = notificationService.getLessonStudents("2838793");
// → DataCacheService.getOgrencilerByDersHarId()
// → NotificationTokenRepository.findByUserIdAndUserType() (DB'den token)

// 3. Bildirim gönder
SendNotificationRequest request = new SendNotificationRequest();
request.setLessonId("2838793");
request.setAcademicId("12345");
request.setTitle("Ders İptali");
request.setBody("Yarınki ders iptal edilmiştir");

SendNotificationResponse response = notificationService.sendBulkNotification(request);
// → Cache'den öğrenci listesi
// → DB'den token'lar
// → ExpoPushService.sendPushNotifications()
// → DB'ye history kaydet
```

## 🎯 Veri Akış Diyagramı

### Öğrenci Listesi Alma

```
Mobile App
    │
    │ GET /api/notifications/lesson/{lessonId}/students
    ▼
NotificationController
    │
    │ getLessonStudents(lessonId)
    ▼
NotificationService
    │
    ├─► DataCacheService.getOgrencilerByDersHarId(lessonId)
    │   └─► Memory Cache (dersOgrencileriMap)
    │       └─► List<Ogrenci> [SOAP'tan gelmiş]
    │
    └─► NotificationTokenRepository.findByUserIdAndUserType()
        └─► Database (notification_tokens)
            └─► hasToken = true/false
```

### Bildirim Gönderme

```
Mobile App
    │
    │ POST /api/notifications/send-bulk
    │ {lessonId, academicId, title, body}
    ▼
NotificationController
    │
    │ sendBulkNotification(request)
    ▼
NotificationService
    │
    ├─► DataCacheService.getOgrencilerByDersHarId()
    │   └─► List<Ogrenci> [öğrenci no'ları]
    │
    ├─► NotificationTokenRepository.findByUserIdIn(studentIds)
    │   └─► List<NotificationToken> [Expo push token'lar]
    │
    ├─► ExpoPushService.sendPushNotifications(tokens, title, body)
    │   └─► Expo API (https://exp.host/--/api/v2/push/send)
    │       └─► Push to devices
    │
    └─► NotificationHistoryRepository.save()
        └─► Database (notification_history)
```

## 🔍 Kritik Noktalar

### 1. Cache Bağımlılığı

```java
if (!cacheService.isInitialized()) {
    // Cache henüz yüklenmedi
    // SOAP'tan veri çekiliyor
    return Collections.emptyList();
}
```

**Önemli:** Uygulama başladığında cache yüklenmesi birkaç dakika sürebilir.

### 2. Token Yönetimi

```java
// Token kayıt (Login sırasında)
POST /api/notifications/register-token
{
  "token": "ExponentPushToken[xxx]",
  "userId": "20180001234",
  "userType": "student"
}

// Token silme (Logout sırasında)
POST /api/notifications/unregister-token
{
  "token": "ExponentPushToken[xxx]"
}
```

### 3. Veri Tutarlılığı

**SOAP → Cache → API**
- Cache periyodik olarak refresh edilir
- Gerçek zamanlı veri için SOAP tek kaynak
- Token'lar DB'de kalıcı

**Örnek Senaryo:**
```
1. Öğrenci derse kaydolur (SOAP'ta)
2. Cache refresh edilir (scheduled job)
3. Akademisyen bildirimi gönderir
4. NotificationService cache'den yeni öğrenciyi görür
5. Eğer öğrencinin token'ı varsa bildirim gider
```

## 📈 Performans Optimizasyonları

### 1. Cache Index'leri

```java
// Hızlı arama için index'ler
private final Map<String, Ders> dersHarIdIndex;
private final Map<String, OgretimElemani> sicilNoIndex;
private final Map<String, List<Ders>> ogrenciDerslerIndex;
```

### 2. Concurrent Collections

```java
// Thread-safe collections
private final List<Ders> allDersler = Collections.synchronizedList(new ArrayList<>());
private final Map<String, List<Ogrenci>> dersOgrencileriMap = new ConcurrentHashMap<>();
```

### 3. Batch Processing

```java
// Expo Push Service - batch gönderim
List<Map<String, Object>> messages = tokens.stream()
    .map(token -> createMessage(token, title, body, data, channelId))
    .collect(Collectors.toList());
```

## 🛡️ Güvenlik Notları

1. **Token Güvenliği:** Token'lar SecureStore'da saklanır (mobil)
2. **API Güvenliği:** Production'da authentication eklenmelidir
3. **Rate Limiting:** Spam önlemek için rate limiting gerekli
4. **Veri Validasyonu:** Tüm input'lar validate edilmelidir

## 📝 Özet

| Veri Tipi | Kaynak | Depolama | Kullanım |
|-----------|--------|----------|----------|
| Öğrenci Bilgileri | SOAP | Memory Cache | Bildirim alıcıları |
| Ders Bilgileri | SOAP | Memory Cache | Ders seçimi |
| Öğretim Elemanı | SOAP | Memory Cache | Yetki kontrolü |
| Push Token'lar | Mobile App | Database | Bildirim gönderimi |
| Bildirim Geçmişi | Backend | Database | Raporlama |

**Sonuç:** Hibrit sistem sayesinde hem gerçek zamanlı veri (SOAP) hem de kalıcı notification yönetimi (DB) sağlanır.
