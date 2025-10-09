# MariaDB Kurulum ve Yapılandırma Rehberi

## 📋 MariaDB Kurulumu

### Windows için

1. **MariaDB İndir ve Kur**
   - [MariaDB Downloads](https://mariadb.org/download/) adresinden indirin
   - MSI installer'ı çalıştırın
   - Root şifresini güçlü bir şifre ile belirleyin
   - "Enable networking" seçeneğini işaretleyin

2. **MariaDB Servisini Başlat**
   ```cmd
   net start MariaDB
   ```

### Linux için

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mariadb-server
sudo systemctl start mariadb
sudo systemctl enable mariadb
sudo mysql_secure_installation

# CentOS/RHEL
sudo yum install mariadb-server
sudo systemctl start mariadb
sudo systemctl enable mariadb
sudo mysql_secure_installation
```

### macOS için

```bash
# Homebrew ile
brew install mariadb
brew services start mariadb
mysql_secure_installation
```

### MySQL Uyumluluğu

MariaDB, MySQL ile %100 uyumludur. MySQL komutlarını kullanabilirsiniz:
```bash
# MariaDB'ye bağlan (mysql komutu ile)
mysql -u root -p
```

## 🔧 Veritabanı Yapılandırması

### 1. MySQL'e Bağlan

```bash
mysql -u root -p
```

### 2. Veritabanı ve Kullanıcı Oluştur

```sql
-- Veritabanı oluştur
CREATE DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Kullanıcı oluştur
CREATE USER 'proliz'@'localhost' IDENTIFIED BY 'güçlü_şifre_buraya';

-- Yetkileri ver
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'proliz'@'localhost';

-- Yetkileri uygula
FLUSH PRIVILEGES;

-- Çıkış
EXIT;
```

### 3. Bağlantıyı Test Et

```bash
mysql -u proliz -p proliz_cache
```

## ⚙️ Uygulama Yapılandırması

### application.properties Güncellemesi

`src/main/resources/application.properties` dosyasında MariaDB ayarları zaten yapılandırılmış durumda:

```properties
# MariaDB Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/proliz_cache?useSSL=false&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8
spring.datasource.driverClassName=org.mariadb.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=güçlü_şifre_buraya
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
```

**Not:** MySQL kullanmak isterseniz, dosyadaki MySQL konfigürasyonunu uncomment edebilirsiniz.

### Şifre Güvenliği

**Önemli:** Şifreyi properties dosyasına yazmak yerine environment variable kullanın:

#### Windows (CMD)
```cmd
set DATABASE_USERNAME=proliz
set DATABASE_PASSWORD=güçlü_şifre_buraya
mvn spring-boot:run
```

#### Windows (PowerShell)
```powershell
$env:DATABASE_USERNAME="proliz"
$env:DATABASE_PASSWORD="güçlü_şifre_buraya"
mvn spring-boot:run
```

#### Linux/macOS
```bash
export DATABASE_USERNAME=proliz
export DATABASE_PASSWORD=güçlü_şifre_buraya
mvn spring-boot:run
```

## 🚀 Uygulamayı Başlatma

### 1. Maven ile Build

```bash
mvn clean install
```

### 2. Uygulamayı Çalıştır

```bash
mvn spring-boot:run
```

### 3. Bağlantıyı Doğrula

Uygulama başladıktan sonra:

```bash
curl http://localhost:8083/ProlizWebServices/api/cache-management/health
```

Beklenen yanıt:
```json
{
  "status": "UP",
  "cacheEnabled": true,
  "hitRate": 0.0,
  "totalItems": 0,
  "healthScore": 100.0
}
```

## 🔍 Veritabanı Kontrolü

### Tabloları Kontrol Et

```sql
USE proliz_cache;

-- Tüm tabloları listele
SHOW TABLES;

-- Beklenen tablolar:
-- cached_soap_responses
-- service_dependencies
-- cache_metrics

-- Tablo yapısını kontrol et
DESCRIBE cached_soap_responses;

-- Kayıt sayısını kontrol et
SELECT COUNT(*) FROM cached_soap_responses;
```

### Cache Verilerini Görüntüle

```sql
-- Son 10 cache kaydı
SELECT 
    id, 
    service_method, 
    cache_key, 
    created_at, 
    expires_at, 
    access_count,
    status
FROM cached_soap_responses 
ORDER BY created_at DESC 
LIMIT 10;

-- Servis metoduna göre istatistikler
SELECT 
    service_method, 
    COUNT(*) as total_count,
    SUM(access_count) as total_accesses,
    AVG(response_size) as avg_size
FROM cached_soap_responses 
GROUP BY service_method;

-- Süresi dolmuş cache'ler
SELECT COUNT(*) 
FROM cached_soap_responses 
WHERE expires_at < NOW() AND status = 'VALID';
```

## 🔧 MySQL Optimizasyonu

### Performans Ayarları

`my.cnf` veya `my.ini` dosyasını düzenleyin:

```ini
[mysqld]
# InnoDB Buffer Pool (RAM'in %70-80'i)
innodb_buffer_pool_size = 2G

# Connection Settings
max_connections = 200
max_allowed_packet = 64M

# Query Cache (MySQL 8.0'da kaldırıldı, 5.7 için)
# query_cache_size = 64M
# query_cache_type = 1

# InnoDB Settings
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT

# Character Set
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
```

MySQL'i yeniden başlatın:

```bash
# Windows
net stop MySQL80
net start MySQL80

# Linux
sudo systemctl restart mysql
```

## 🗄️ Backup ve Restore

### Backup Oluşturma

```bash
# Tam backup
mysqldump -u proliz -p proliz_cache > backup_$(date +%Y%m%d_%H%M%S).sql

# Sadece yapı (data olmadan)
mysqldump -u proliz -p --no-data proliz_cache > schema_backup.sql

# Sadece belirli tablolar
mysqldump -u proliz -p proliz_cache cached_soap_responses > cache_backup.sql
```

### Backup'ı Geri Yükleme

```bash
# Tam restore
mysql -u proliz -p proliz_cache < backup_20251007_093000.sql

# Veritabanını sıfırlayıp restore
mysql -u root -p -e "DROP DATABASE IF EXISTS proliz_cache; CREATE DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u proliz -p proliz_cache < backup_20251007_093000.sql
```

### Otomatik Backup (Cron)

Linux/macOS için crontab ekleyin:

```bash
crontab -e
```

Her gece 2:00'de backup:
```cron
0 2 * * * /usr/bin/mysqldump -u proliz -pŞİFRE proliz_cache > /backup/proliz_$(date +\%Y\%m\%d).sql
```

## 🛠️ Troubleshooting

### Bağlantı Hatası

**Hata:** `Communications link failure`

**Çözüm:**
```bash
# MariaDB servisinin çalıştığını kontrol et
# Windows
sc query MariaDB

# Linux
sudo systemctl status mariadb

# MariaDB'yi başlat
# Windows
net start MariaDB

# Linux
sudo systemctl start mariadb
```

### Authentication Hatası

**Hata:** `Access denied for user 'proliz'@'localhost'`

**Çözüm:**
```sql
-- Root olarak bağlan
mysql -u root -p

-- Kullanıcıyı kontrol et
SELECT user, host FROM mysql.user WHERE user='proliz';

-- Şifreyi sıfırla
ALTER USER 'proliz'@'localhost' IDENTIFIED BY 'yeni_şifre';
FLUSH PRIVILEGES;
```

### Timezone Hatası

**Hata:** `The server time zone value 'XXX' is unrecognized`

**Çözüm:**
```sql
-- MySQL timezone tablolarını doldur
mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root -p mysql

-- Veya connection string'de timezone belirt (zaten yapılmış):
-- ?serverTimezone=Europe/Istanbul
```

### Too Many Connections

**Hata:** `Too many connections`

**Çözüm:**
```sql
-- Mevcut bağlantıları kontrol et
SHOW PROCESSLIST;

-- Max connections'ı artır
SET GLOBAL max_connections = 300;

-- Kalıcı olarak my.cnf'de:
-- max_connections = 300
```

## 📊 Monitoring

### Bağlantı Durumu

```sql
-- Aktif bağlantılar
SHOW PROCESSLIST;

-- Bağlantı istatistikleri
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- Veritabanı boyutu
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'proliz_cache'
GROUP BY table_schema;
```

### Performans Metrikleri

```sql
-- InnoDB buffer pool kullanımı
SHOW STATUS LIKE 'Innodb_buffer_pool%';

-- Query cache (MySQL 5.7)
SHOW STATUS LIKE 'Qcache%';

-- Slow queries
SHOW STATUS LIKE 'Slow_queries';
```

## ✅ Kurulum Checklist

- [ ] MariaDB 10.5+ kurulu ve çalışıyor
- [ ] `proliz_cache` veritabanı oluşturuldu
- [ ] `proliz` kullanıcısı oluşturuldu ve yetkilendirildi (veya root kullanıcısı)
- [ ] Bağlantı test edildi
- [ ] `application.properties` dosyası güncellendi
- [ ] Şifre environment variable olarak ayarlandı
- [ ] Uygulama başarıyla başladı
- [ ] Health check PASSED
- [ ] Tablolar otomatik oluşturuldu
- [ ] İlk cache verisi yazıldı

## 🔄 MySQL'den MariaDB'ye Geçiş

MariaDB, MySQL'in drop-in replacement'ıdır. Mevcut MySQL veritabanınız varsa:

1. **Veriyi Yedekle**
   ```bash
   mysqldump -u root -p proliz_cache > backup.sql
   ```

2. **MariaDB'yi Kur**
   ```bash
   # Yukarıdaki kurulum adımlarını takip edin
   ```

3. **Veriyi Geri Yükle**
   ```bash
   mysql -u root -p proliz_cache < backup.sql
   ```

4. **application.properties Güncelle**
   ```properties
   spring.datasource.url=jdbc:mariadb://localhost:3306/proliz_cache...
   spring.datasource.driverClassName=org.mariadb.jdbc.Driver
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
   ```

**Not:** Kod değişikliği gerekmez! MariaDB MySQL ile %100 uyumludur.

## 🔐 Güvenlik Önerileri

1. **Güçlü Şifreler Kullanın**
   - En az 16 karakter
   - Büyük/küçük harf, rakam ve özel karakter içermeli

2. **Root Erişimini Kısıtlayın**
   ```sql
   -- Root'un sadece localhost'tan bağlanmasına izin ver
   DELETE FROM mysql.user WHERE user='root' AND host!='localhost';
   FLUSH PRIVILEGES;
   ```

3. **Gereksiz Kullanıcıları Silin**
   ```sql
   SELECT user, host FROM mysql.user;
   DROP USER 'gereksiz_kullanici'@'localhost';
   ```

4. **SSL Bağlantı Kullanın (Production)**
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/proliz_cache?useSSL=true&requireSSL=true
   ```

5. **Firewall Kuralları**
   - MySQL portunu (3306) sadece gerekli IP'lere açın

## 📞 Destek

Sorun yaşarsanız:
1. MySQL error log'unu kontrol edin: `/var/log/mysql/error.log`
2. Uygulama loglarını kontrol edin: `logs/proliz-web-services.log`
3. GitHub Issues'a bildirin
