# Push Notification Sistemi - Backend Setup

## 🏗️ Sistem Mimarisi

### Veri Akışı

```
SOAP Web Service (Öğrenci Bilgi Sistemi)
           ↓
    DataCacheService (Memory Cache)
           ↓
    NotificationService (Bildirim Yönetimi)
           ↓
    Expo Push Service (Bildirim Gönderimi)
```

### Önemli Not: Hibrit Veri Yönetimi

Bu proje **hibrit bir veri yönetimi** kullanır:

1. **SOAP'tan Gelen Veriler (Cache'de):**
   - Öğrenci bilgileri
   - Ders bilgileri
   - Öğretim elemanı bilgileri
   - **Bu veriler veritabanına YAZILMAZ, sadece memory'de cache'lenir**

2. **Veritabanına Yazılan Veriler:**
   - Push notification token'ları
   - Bildirim geçmişi
   - **Sadece notification ile ilgili veriler DB'ye yazılır**

## ✅ Tamamlanan İşlemler

Backend projenize push notification sistemi başarıyla eklendi!

### Oluşturulan Dosyalar

#### 1. Entity Classes
- `NotificationToken.java` - Cihaz token'larını saklar
- `NotificationHistory.java` - Bildirim geçmişini saklar

#### 2. Repository Classes
- `NotificationTokenRepository.java` - Token CRUD işlemleri
- `NotificationHistoryRepository.java` - Geçmiş CRUD işlemleri

#### 3. Model Classes (DTO)
- `TokenRegistrationRequest.java` - Token kayıt isteği
- `SendNotificationRequest.java` - Bildirim gönderme isteği
- `SendNotificationResponse.java` - Bildirim gönderme yanıtı
- `LessonInfo.java` - Ders bilgisi
- `StudentInfo.java` - Öğrenci bilgisi

#### 4. Service Classes
- `ExpoPushService.java` - Expo Push Service entegrasyonu
- `NotificationService.java` - Bildirim iş mantığı

#### 5. Controller
- `NotificationController.java` - REST API endpoint'leri

#### 6. Database
- `database_notification_schema.sql` - Veritabanı şeması

## 🚀 Kurulum Adımları

### 1. Veritabanını Oluştur

**ÖNEMLİ:** Mevcut `proliz_cache` veritabanını kullanacağız. Ayrı veritabanı oluşturmaya gerek yok.

```bash
# MySQL/MariaDB'ye bağlan
mysql -u root -p

# Mevcut veritabanını kullan
USE proliz_cache;

# Notification tablolarını oluştur
CREATE TABLE IF NOT EXISTS notification_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    platform VARCHAR(20),
    device_id VARCHAR(100),
    os_version VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, user_type),
    INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    academic_id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    class_id VARCHAR(20),
    recipient_count INT DEFAULT 0,
    sent_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_academic (academic_id),
    INDEX idx_lesson (lesson_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Neden Ayrı Veritabanı Yok?**
- Öğrenci/ders verileri zaten SOAP'tan geliyor ve cache'de tutuluyor
- Sadece token ve history bilgileri DB'ye yazılıyor
- Mevcut `proliz_cache` veritabanı yeterli

### 2. Application Properties Kontrol

`application.properties` dosyanız zaten doğru şekilde yapılandırılmış:

```properties
# Mevcut ayarlar - DEĞİŞTİRMEYİN
spring.datasource.url=jdbc:mariadb://localhost:3306/proliz_cache?useSSL=false&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=sahinbey_
spring.jpa.hibernate.ddl-auto=update
```

**Notification tabloları otomatik oluşturulacak** (JPA `ddl-auto=update` sayesinde).

**Veri Akışı:**
1. **SOAP Service** → Öğrenci/Ders verileri → **DataCacheService (Memory)**
2. **Mobile App** → Token kayıt → **NotificationToken (Database)**
3. **Academic** → Bildirim gönder → **NotificationHistory (Database)**
4. **NotificationService** → Cache'den öğrenci listesi → Token'larla eşleştir → Bildirim gönder

### 3. Projeyi Derle

```bash
cd C:\Users\cdikici\IdeaProjects\ProlizWebServices
mvn clean install
```

### 4. Uygulamayı Başlat

```bash
mvn spring-boot:run
```

Veya IDE'den `ProlizWebServicesApplication.java` dosyasını çalıştırın.

## 📡 API Endpoint'leri

Tüm endpoint'ler `/ProlizWebServices/api/notifications` altında:

### Token Yönetimi

**Token Kaydet**
```
POST /api/notifications/register-token
Content-Type: application/json

{
  "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
  "userId": "20180001234",
  "userType": "student",
  "platform": "android",
  "deviceId": "Samsung Galaxy S21",
  "osVersion": "13"
}
```

**Token Sil**
```
POST /api/notifications/unregister-token
Content-Type: application/json

{
  "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"
}
```

### Ders ve Öğrenci Bilgileri

**Akademisyenin Derslerini Getir**
```
GET /api/notifications/academic/{sicilNo}/lessons
```

**Ders Öğrencilerini Getir**
```
GET /api/notifications/lesson/{dersHarID}/students
```

**Sınıf Öğrencilerini Getir**
```
GET /api/notifications/lesson/{dersHarID}/class/{classId}/students
```

### Bildirim Gönderme

**Toplu Bildirim (Tüm Ders)**
```
POST /api/notifications/send-bulk
Content-Type: application/json

{
  "title": "Ders İptali",
  "body": "Yarınki ders iptal edilmiştir.",
  "data": {
    "screen": "LessonsScreen",
    "lessonId": "2838793"
  },
  "recipientType": "all",
  "lessonId": "2838793",
  "academicId": "12345"
}
```

**Sınıf Bazlı Bildirim**
```
POST /api/notifications/send-class
Content-Type: application/json

{
  "title": "Sınıf Duyurusu",
  "body": "1A sınıfı için önemli duyuru",
  "recipientType": "class",
  "lessonId": "2838793",
  "classId": "1A",
  "academicId": "12345"
}
```

**Bireysel Bildirim**
```
POST /api/notifications/send-individual
Content-Type: application/json

{
  "title": "Kişisel Mesaj",
  "body": "Proje teslim tarihi yaklaşıyor",
  "recipientType": "individual",
  "lessonId": "2838793",
  "studentIds": ["20180001234", "20180001235"],
  "academicId": "12345"
}
```

**Bildirim Geçmişi**
```
GET /api/notifications/history/{sicilNo}?limit=50
```

## 🧪 Test Etme

### 1. Swagger UI ile Test

Uygulama çalıştıktan sonra:

```
http://localhost:8083/ProlizWebServices/swagger-ui.html
```

"Push Notifications" bölümünden tüm endpoint'leri test edebilirsiniz.

### 2. cURL ile Test

**Token Kayıt Testi:**
```bash
curl -X POST http://localhost:8083/ProlizWebServices/api/notifications/register-token \
  -H "Content-Type: application/json" \
  -d '{
    "token": "ExponentPushToken[test123]",
    "userId": "20180001234",
    "userType": "student",
    "platform": "android"
  }'
```

**Akademisyen Dersleri Testi:**
```bash
curl http://localhost:8083/ProlizWebServices/api/notifications/academic/12345/lessons
```

**Bildirim Gönderme Testi:**
```bash
curl -X POST http://localhost:8083/ProlizWebServices/api/notifications/send-bulk \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Bildirimi",
    "body": "Bu bir test mesajıdır",
    "recipientType": "all",
    "lessonId": "2838793",
    "academicId": "12345"
  }'
```

### 3. Postman Collection

Postman için import edilebilir collection oluşturabilirsiniz veya Swagger'dan otomatik generate edebilirsiniz.

## 🔗 Mobil Uygulama Entegrasyonu

Mobil uygulamanızda API URL'lerini güncelleyin:

```typescript
// notification.service.ts içinde
const getBaseUrl = () => {
  if (Platform.OS === 'android') {
    return 'http://10.0.2.2:8083/ProlizWebServices/api/notifications';
  }
  return 'http://localhost:8083/ProlizWebServices/api/notifications';
};
```

## 📊 Veritabanı Sorguları

**Token İstatistikleri:**
```sql
SELECT user_type, COUNT(*) as token_count 
FROM notification_tokens 
GROUP BY user_type;
```

**Akademisyen Bildirim İstatistikleri:**
```sql
SELECT 
    academic_id,
    COUNT(*) as total_notifications,
    SUM(sent_count) as total_sent,
    SUM(failed_count) as total_failed
FROM notification_history 
GROUP BY academic_id;
```

**Son Bildirimler:**
```sql
SELECT * FROM notification_history 
ORDER BY created_at DESC 
LIMIT 10;
```

## 🔧 Sorun Giderme

### Veritabanı Bağlantı Hatası
```
Error: Could not connect to database
```
**Çözüm:** MariaDB/MySQL'in çalıştığından ve şifrenin doğru olduğundan emin olun.

### Cache Not Initialized
```
Cache not initialized yet
```
**Çözüm:** Uygulamanın tamamen başlamasını bekleyin. Cache yüklenmesi birkaç dakika sürebilir.

### Expo Push Service Hatası
```
Error sending push notification
```
**Çözüm:** 
- Internet bağlantısını kontrol edin
- Token'ların geçerli olduğundan emin olun
- Expo Push Service limitlerini kontrol edin

## 📝 Notlar

1. **Güvenlik:** Production'da authentication/authorization eklenmelidir
2. **Rate Limiting:** Spam önlemek için rate limiting eklenmelidir
3. **Monitoring:** Bildirim başarı oranlarını izleyin
4. **Cleanup:** Eski token'ları ve geçmişi periyodik olarak temizleyin
5. **Scaling:** Yüksek yük için Redis queue kullanabilirsiniz

## 🎯 Sonraki Adımlar

- [ ] Authentication/Authorization ekle
- [ ] Rate limiting ekle
- [ ] Bildirim şablonları oluştur
- [ ] Zamanlanmış bildirimler
- [ ] Bildirim istatistik dashboard'u
- [ ] Email/SMS fallback mekanizması

## 📚 Kaynaklar

- [Expo Push Notifications](https://docs.expo.dev/push-notifications/overview/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MariaDB Documentation](https://mariadb.org/documentation/)
