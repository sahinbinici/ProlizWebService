# Deployment Rehberi - 193.140.136.26 Sunucusu

## 📋 Ön Hazırlık

### Sunucu Bilgileri
- **IP Adresi**: 193.140.136.26
- **Uygulama Portu**: 8083
- **Context Path**: /ProlizWebServices

### Gerekli Yazılımlar
- Java 17+
- MariaDB/MySQL 8.0+
- Redis 6.0+ (opsiyonel ama önerilen)
- Maven 3.6+ (build için)

---

## 🚀 Deployment Yöntemleri

### Yöntem 1: Docker Compose (ÖNERİLEN)

#### 1. Projeyi Sunucuya Kopyala
```bash
# Yerel bilgisayardan
scp -r ProlizWebServices/ user@193.140.136.26:/opt/

# Veya git ile
ssh user@193.140.136.26
cd /opt
git clone <repository-url> ProlizWebServices
cd ProlizWebServices
```Savantek202!45

#### 2. Environment Variables Ayarla
```bash
# .env dosyası oluştur
cp .env.example .env
nano .env
```

**.env dosyası içeriği:**
```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=güçlü_root_şifresi_buraya
MYSQL_PASSWORD=güçlü_mysql_şifresi_buraya

# SOAP Service Credentials
SOAP_USERNAME=ProLmsGan
SOAP_PASSWORD=-2020+Pro*Gan#

# Redis Password (opsiyonel)
REDIS_PASSWORD=

# Application Settings
SPRING_PROFILES_ACTIVE=docker
LOG_LEVEL_APP=INFO
```

#### 3. Docker Compose ile Başlat
```bash
# Servisleri başlat
docker-compose up -d

# Logları takip et
docker-compose logs -f app

# Servislerin durumunu kontrol et
docker-compose ps
```

#### 4. Erişim Kontrolleri
```bash
# Health check
curl http://193.140.136.26:8083/ProlizWebServices/api/cache-management/health

# Swagger UI
# Tarayıcıda: http://193.140.136.26:8083/ProlizWebServices/swagger-ui.html
```

---

### Yöntem 2: Standalone Deployment

#### 1. Sunucuda Gerekli Servisleri Kur

**MariaDB Kurulumu:**
```bash
sudo apt update
sudo apt install mariadb-server -y

# MariaDB'yi başlat
sudo systemctl start mariadb
sudo systemctl enable mariadb

# Güvenlik ayarları
sudo mysql_secure_installation
```

**Redis Kurulumu (Opsiyonel):**
```bash
sudo apt install redis-server -y
sudo systemctl start redis
sudo systemctl enable redis
```

#### 2. Veritabanı Oluştur
```bash
sudo mysql -u root -p
```

```sql
CREATE DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'proliz'@'localhost' IDENTIFIED BY 'güçlü_şifre_buraya';
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'proliz'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 3. Uygulama Dizinlerini Oluştur
```bash
sudo mkdir -p /opt/proliz/{cache,logs,data}
sudo chown -R $USER:$USER /opt/proliz
```

#### 4. Projeyi Build Et
```bash
cd /path/to/ProlizWebServices
mvn clean package -DskipTests
```

#### 5. WAR Dosyasını Sunucuya Kopyala
```bash
# Yerel bilgisayardan
scp target/ProlizWebServices-0.0.1-SNAPSHOT.war user@193.140.136.26:/opt/proliz/app.war
```

#### 6. Environment Variables Ayarla
```bash
# /etc/environment dosyasına ekle
sudo nano /etc/environment
```

Eklenecek satırlar:
```bash
DATABASE_PASSWORD="güçlü_şifre_buraya"
SOAP_SERVICE_USERNAME="ProLmsGan"
SOAP_SERVICE_PASSWORD="-2020+Pro*Gan#"
REDIS_HOST="193.140.136.26"
LOG_FILE_PATH="/opt/proliz/logs/proliz-web-services.log"
CACHE_DISK_DIR="/opt/proliz/cache"
```

Değişiklikleri yükle:
```bash
source /etc/environment
```

#### 7. Systemd Service Oluştur
```bash
sudo nano /etc/systemd/system/proliz.service
```

**Service dosyası içeriği:**
```ini
[Unit]
Description=Proliz Web Services
After=network.target mariadb.service redis.service
Wants=mariadb.service redis.service

[Service]
Type=simple
User=proliz
Group=proliz
WorkingDirectory=/opt/proliz

# Environment Variables
Environment="DATABASE_PASSWORD=güçlü_şifre_buraya"
Environment="SOAP_SERVICE_USERNAME=ProLmsGan"
Environment="SOAP_SERVICE_PASSWORD=-2020+Pro*Gan#"
Environment="REDIS_HOST=193.140.136.26"
Environment="LOG_FILE_PATH=/opt/proliz/logs/proliz-web-services.log"
Environment="CACHE_DISK_DIR=/opt/proliz/cache"

# JVM Options
Environment="JAVA_OPTS=-Xmx4G -Xms1G -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/proliz/logs"

# Start Command
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/proliz/app.war

# Restart Policy
Restart=always
RestartSec=10

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=proliz

[Install]
WantedBy=multi-user.target
```

#### 8. Service'i Başlat
```bash
# User oluştur
sudo useradd -r -s /bin/false proliz
sudo chown -R proliz:proliz /opt/proliz

# Service'i aktif et
sudo systemctl daemon-reload
sudo systemctl enable proliz
sudo systemctl start proliz

# Durumu kontrol et
sudo systemctl status proliz

# Logları takip et
sudo journalctl -u proliz -f
```

---

## 🔒 Güvenlik Ayarları

### 1. Firewall Konfigürasyonu
```bash
# UFW kurulu değilse
sudo apt install ufw -y

# Port 8083'ü aç (uygulama)
sudo ufw allow 8083/tcp

# SSH portunu aç (22)
sudo ufw allow 22/tcp

# Firewall'ı aktif et
sudo ufw enable

# Durumu kontrol et
sudo ufw status
```

### 2. Nginx Reverse Proxy (Opsiyonel - HTTPS için)
```bash
sudo apt install nginx -y
sudo nano /etc/nginx/sites-available/proliz
```

**Nginx konfigürasyonu:**
```nginx
server {
    listen 80;
    server_name 193.140.136.26;

    location /ProlizWebServices {
        proxy_pass http://localhost:8083/ProlizWebServices;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # CORS headers (if needed)
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Content-Type, Authorization' always;
    }
}
```

Aktif et:
```bash
sudo ln -s /etc/nginx/sites-available/proliz /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 3. SSL/TLS Sertifikası (Let's Encrypt)
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d yourdomain.com
```

---

## 📊 İlk Çalıştırma ve Test

### 1. Health Check
```bash
curl http://193.140.136.26:8083/ProlizWebServices/api/cache-management/health
```

**Beklenen Yanıt:**
```json
{
  "status": "UP",
  "cacheEnabled": true,
  "healthScore": 100.0
}
```

### 2. Swagger UI Erişimi
Tarayıcıda açın:
```
http://193.140.136.26:8083/ProlizWebServices/swagger-ui.html
```

### 3. İlk Veri Yükleme
```bash
# Ders listesi (ilk kez ~30-60 dakika sürer)
curl "http://193.140.136.26:8083/ProlizWebServices/api/data/dersler?page=0&size=10"
```

### 4. Cache İstatistikleri
```bash
curl http://193.140.136.26:8083/ProlizWebServices/api/cache-management/statistics
```

### 5. Progressive Loading Durumu
```bash
curl http://193.140.136.26:8083/ProlizWebServices/api/data/cache/progressive-status
```

---

## 🔧 Yapılandırma Özeti

### Güncellenmiş Ayarlar

#### application.properties
```properties
# Server
server.port=8083

# Redis (sunucu IP'si)
spring.data.redis.host=193.140.136.26

# Database (sunucu IP'si)
spring.datasource.url=jdbc:mariadb://193.140.136.26:3306/proliz_cache

# Cache Directory (mutlak path)
cache.disk.directory=/opt/proliz/cache

# Logs (mutlak path)
logging.file.name=/opt/proliz/logs/proliz-web-services.log
```

#### CORS Ayarları
Tüm controller'larda güncellendi:
- `http://193.140.136.26:8083`
- `https://193.140.136.26:8083`
- `http://193.140.136.26`
- `https://193.140.136.26`
- `http://localhost:8083` (test için)
- `http://localhost:8080` (test için)

---

## 🚨 Sorun Giderme

### Port Çakışması
```bash
# Port 8083 kullanımda mı?
sudo netstat -tulpn | grep 8083

# Kullanılan portu öldür
sudo kill -9 <PID>
```

### Database Bağlantı Hatası
```bash
# MariaDB çalışıyor mu?
sudo systemctl status mariadb

# Bağlantıyı test et
mysql -u proliz -p proliz_cache

# Logları kontrol et
sudo tail -f /var/log/mysql/error.log
```

### Redis Bağlantı Hatası
```bash
# Redis çalışıyor mu?
sudo systemctl status redis

# Bağlantıyı test et
redis-cli ping

# Redis'i devre dışı bırak (gerekirse)
# application.properties'de:
cache.strategy.redis.enabled=false
```

### Memory Hatası
```bash
# JVM heap size artır
# /etc/systemd/system/proliz.service dosyasında:
Environment="JAVA_OPTS=-Xmx8G -Xms2G ..."

sudo systemctl daemon-reload
sudo systemctl restart proliz
```

### Uygulama Logları
```bash
# Systemd logs
sudo journalctl -u proliz -f

# Application logs
tail -f /opt/proliz/logs/proliz-web-services.log

# Son 100 satır
tail -n 100 /opt/proliz/logs/proliz-web-services.log
```

---

## 📈 Performans İzleme

### Sistem Kaynakları
```bash
# CPU ve Memory kullanımı
htop

# Disk kullanımı
df -h
du -sh /opt/proliz/*

# Network bağlantıları
sudo netstat -tulpn | grep java
```

### Cache Metrikleri
```bash
# Cache istatistikleri
curl http://193.140.136.26:8083/ProlizWebServices/api/cache-management/statistics | jq

# Health score
curl http://193.140.136.26:8083/ProlizWebServices/api/cache-management/health | jq
```

### Redis Monitoring
```bash
redis-cli info stats
redis-cli info memory
```

### Database Monitoring
```sql
-- Bağlantı sayısı
SHOW PROCESSLIST;

-- Tablo boyutları
SELECT 
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'proliz_cache'
ORDER BY (data_length + index_length) DESC;
```

---

## 🔄 Güncelleme ve Bakım

### Uygulama Güncelleme
```bash
# 1. Yeni WAR dosyasını kopyala
scp target/ProlizWebServices-0.0.1-SNAPSHOT.war user@193.140.136.26:/opt/proliz/app.war.new

# 2. Sunucuda
ssh user@193.140.136.26
cd /opt/proliz

# 3. Uygulamayı durdur
sudo systemctl stop proliz

# 4. Yedek al
cp app.war app.war.backup

# 5. Yeni versiyonu aktif et
mv app.war.new app.war

# 6. Uygulamayı başlat
sudo systemctl start proliz

# 7. Logları kontrol et
sudo journalctl -u proliz -f
```

### Cache Temizleme
```bash
# Tüm cache'i temizle
curl -X DELETE http://193.140.136.26:8083/ProlizWebServices/api/cache-management/invalidate

# Disk cache'i manuel temizle
rm -rf /opt/proliz/cache/*

# Database cache'i temizle
mysql -u proliz -p proliz_cache -e "TRUNCATE TABLE cache_entry;"
```

### Veritabanı Yedekleme
```bash
# Yedek al
mysqldump -u proliz -p proliz_cache > /opt/proliz/backups/proliz_cache_$(date +%Y%m%d_%H%M%S).sql

# Yedekten geri yükle
mysql -u proliz -p proliz_cache < /opt/proliz/backups/proliz_cache_20251020_101800.sql
```

---

## ✅ Deployment Checklist

- [ ] Java 17 kurulu ve çalışıyor
- [ ] MariaDB kurulu ve yapılandırılmış
- [ ] Redis kurulu (opsiyonel)
- [ ] Veritabanı oluşturuldu (proliz_cache)
- [ ] Dizinler oluşturuldu (/opt/proliz/*)
- [ ] Environment variables ayarlandı
- [ ] Firewall portları açıldı (8083)
- [ ] WAR dosyası sunucuya kopyalandı
- [ ] Systemd service oluşturuldu
- [ ] Uygulama başarıyla başladı
- [ ] Health check PASSED
- [ ] Swagger UI erişilebilir
- [ ] CORS ayarları test edildi
- [ ] İlk cache yükleme başladı
- [ ] Loglar düzgün yazılıyor
- [ ] Monitoring kuruldu

---

## 📞 Erişim Bilgileri

### API Endpoints
- **Base URL**: `http://193.140.136.26:8083/ProlizWebServices`
- **Swagger UI**: `http://193.140.136.26:8083/ProlizWebServices/swagger-ui.html`
- **Health Check**: `http://193.140.136.26:8083/ProlizWebServices/api/cache-management/health`
- **Cache Stats**: `http://193.140.136.26:8083/ProlizWebServices/api/cache-management/statistics`

### Örnek API Çağrıları
```bash
# Ders listesi
curl "http://193.140.136.26:8083/ProlizWebServices/api/data/dersler?page=0&size=10"

# Öğrenci girişi
curl -X POST "http://193.140.136.26:8083/ProlizWebServices/api/ogrenci/sifre-kontrol?ogrenciNo=20230001&sifre=password123"

# Öğretim elemanı
curl "http://193.140.136.26:8083/ProlizWebServices/api/data/ogretim-elemani/2838793"
```

---

## 🎯 Sonraki Adımlar

1. **SSL/HTTPS Aktif Et** - Let's Encrypt ile ücretsiz sertifika
2. **Monitoring Ekle** - Prometheus + Grafana
3. **Backup Stratejisi** - Otomatik yedekleme (cron job)
4. **Log Rotation** - Logrotate konfigürasyonu
5. **Rate Limiting** - API rate limiting ekle
6. **CDN** - Statik dosyalar için CDN kullan

---

**Deployment tamamlandı! 🎉**

Sorularınız için: dev@proliz.edu.tr
