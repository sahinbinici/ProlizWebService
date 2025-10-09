# Push Notification - Hızlı Başlangıç Kılavuzu

## 🚀 5 Dakikada Çalıştır

### 1. Veritabanı Tablolarını Oluştur (30 saniye)

```bash
mysql -u root -p
```

```sql
USE proliz_cache;

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
    INDEX idx_lesson (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2. Backend'i Başlat (1 dakika)

```bash
cd C:\Users\cdikici\IdeaProjects\ProlizWebServices
mvn clean install
mvn spring-boot:run
```

**Bekle:** Cache yüklenene kadar (~2-3 dakika)

### 3. API'leri Test Et (2 dakika)

**Swagger UI:**
```
http://localhost:8083/ProlizWebServices/swagger-ui.html
```

**Test Token Kaydet:**
```bash
curl -X POST http://localhost:8083/ProlizWebServices/api/notifications/register-token \
  -H "Content-Type: application/json" \
  -d '{
    "token": "ExponentPushToken[test123]",
    "userId": "12345",
    "userType": "academic",
    "platform": "android"
  }'
```

**Akademisyen Derslerini Getir:**
```bash
curl http://localhost:8083/ProlizWebServices/api/notifications/academic/12345/lessons
```

### 4. Mobil Uygulamayı Başlat (1 dakika)

```bash
cd C:\Users\cdikici\Desktop\GaunMobil
npm start
```

## ✅ Kontrol Listesi

- [ ] MariaDB çalışıyor
- [ ] `proliz_cache` veritabanı var
- [ ] `notification_tokens` tablosu oluşturuldu
- [ ] `notification_history` tablosu oluşturuldu
- [ ] Backend başladı (port 8083)
- [ ] Cache yüklendi (log'larda "Cache initialized" mesajı)
- [ ] Swagger UI açılıyor
- [ ] Mobil uygulama başladı

## 🔍 Sorun Giderme

### Backend Başlamıyor

**Hata:** `Could not connect to database`

**Çözüm:**
```bash
# MariaDB'yi başlat
net start MySQL
# veya
sudo systemctl start mariadb
```

### Cache Yüklenmiyor

**Hata:** `Cache not initialized`

**Çözüm:** 
- SOAP servisinin çalıştığından emin olun
- Log'larda hata mesajlarını kontrol edin
- 2-3 dakika bekleyin (progressive loading)

### Bildirim Gönderilmiyor

**Kontrol:**
1. Token kayıtlı mı?
   ```sql
   SELECT * FROM notification_tokens;
   ```

2. Öğrenci cache'de mi?
   ```
   GET /api/notifications/lesson/{lessonId}/students
   ```

3. Expo Push Service'e erişim var mı?
   - Internet bağlantısı
   - Firewall ayarları

## 📊 Test Senaryosu

### Senaryo 1: Token Kayıt

```bash
# 1. Token kaydet
curl -X POST http://localhost:8083/ProlizWebServices/api/notifications/register-token \
  -H "Content-Type: application/json" \
  -d '{
    "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
    "userId": "20180001234",
    "userType": "student",
    "platform": "android",
    "deviceId": "Samsung Galaxy S21",
    "osVersion": "13"
  }'

# 2. Kontrol et
mysql -u root -p -e "SELECT * FROM proliz_cache.notification_tokens WHERE user_id='20180001234';"
```

### Senaryo 2: Akademisyen Dersleri

```bash
# 1. Akademisyenin derslerini getir
curl http://localhost:8083/ProlizWebServices/api/notifications/academic/12345/lessons

# Beklenen response:
# [
#   {
#     "lessonId": "2838793",
#     "lessonCode": "BIL101",
#     "lessonName": "Programlamaya Giriş",
#     "academicId": "12345",
#     "studentCount": 45,
#     "classes": ["1A", "1B"]
#   }
# ]
```

### Senaryo 3: Bildirim Gönder

```bash
# 1. Ders öğrencilerini kontrol et
curl http://localhost:8083/ProlizWebServices/api/notifications/lesson/2838793/students

# 2. Bildirim gönder
curl -X POST http://localhost:8083/ProlizWebServices/api/notifications/send-bulk \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Bildirimi",
    "body": "Bu bir test mesajıdır",
    "data": {
      "screen": "LessonsScreen",
      "lessonId": "2838793"
    },
    "recipientType": "all",
    "lessonId": "2838793",
    "academicId": "12345"
  }'

# 3. Geçmişi kontrol et
curl http://localhost:8083/ProlizWebServices/api/notifications/history/12345
```

## 📱 Mobil Uygulama Testi

### 1. Token Otomatik Kayıt

- Uygulamaya giriş yap
- Token otomatik kaydedilir
- Log'larda "Token registered" mesajı

### 2. Bildirim Gönderme Ekranı

- Akademik personel olarak giriş yap
- "Bildirim Gönder" ekranına git
- Dersini seç (backend'den gelir)
- Alıcı tipini seç
- Mesaj yaz ve gönder

### 3. Bildirim Alma

- Öğrenci olarak giriş yap
- Bildirim geldiğinde notification görünür
- Tıkla → İlgili ekrana yönlendirilir

## 🎯 Başarı Kriterleri

✅ Backend başladı ve cache yüklendi
✅ Token kayıt API'si çalışıyor
✅ Akademisyen dersleri API'si cache'den veri dönüyor
✅ Öğrenci listesi API'si cache'den veri dönüyor
✅ Bildirim gönderme API'si çalışıyor
✅ Expo Push Service'e istek gidiyor
✅ Bildirim geçmişi kaydediliyor
✅ Mobil uygulama token kaydediyor
✅ Mobil uygulama bildirim alabiliyor

## 📚 Daha Fazla Bilgi

- **Mimari:** `NOTIFICATION_ARCHITECTURE.md`
- **Detaylı Setup:** `NOTIFICATION_SETUP.md`
- **Mobil Uygulama:** `GaunMobil/PUSH_NOTIFICATION_GUIDE.md`

## 🆘 Yardım

**Log Dosyaları:**
```bash
# Backend logs
tail -f C:\Users\cdikici\IdeaProjects\ProlizWebServices\logs\proliz-web-services.log

# Mobil app logs
# Metro bundler console'da görünür
```

**Veritabanı Kontrol:**
```sql
-- Token sayısı
SELECT user_type, COUNT(*) FROM proliz_cache.notification_tokens GROUP BY user_type;

-- Son bildirimler
SELECT * FROM proliz_cache.notification_history ORDER BY created_at DESC LIMIT 10;

-- Akademisyen istatistikleri
SELECT academic_id, COUNT(*) as total, SUM(sent_count) as sent 
FROM proliz_cache.notification_history 
GROUP BY academic_id;
```
