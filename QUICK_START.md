# 🚀 Hızlı Başlangıç Rehberi

## 📦 Kurulum (5 Dakika)

### 1️⃣ MariaDB Kurulumu

```bash
# MariaDB'ye bağlan (mysql komutu ile - uyumlu)
mysql -u root -p

# Veritabanı oluştur
CREATE DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'proliz'@'localhost' IDENTIFIED BY 'proliz123';
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'proliz'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Not:** MariaDB yoksa [MYSQL_SETUP.md](MYSQL_SETUP.md) dosyasından kurulum yapın.

### 2️⃣ Şifreyi Ayarla

**Windows (CMD):**
```cmd
set DATABASE_PASSWORD=proliz123
```

**Windows (PowerShell):**
```powershell
$env:DATABASE_PASSWORD="proliz123"
```

**Linux/macOS:**
```bash
export DATABASE_PASSWORD=proliz123
```

### 3️⃣ Uygulamayı Başlat

```bash
mvn spring-boot:run
```

### 4️⃣ Test Et

Tarayıcıda aç: http://localhost:8083/ProlizWebServices/swagger-ui.html

## ✅ Başarılı Kurulum Kontrolü

### Health Check
```bash
curl http://localhost:8083/ProlizWebServices/api/cache-management/health
```

**Beklenen Yanıt:**
```json
{
  "status": "UP",
  "cacheEnabled": true,
  "healthScore": 100.0
}
```

## 🎯 İlk API Çağrısı

### Ders Listesini Çek

```bash
curl http://localhost:8083/ProlizWebServices/api/data/dersler?page=0&size=10
```

**İlk çağrı:** ~10-30 saniye (SOAP'tan çekilir)  
**Sonraki çağrılar:** ~1-10ms (Cache'ten gelir) ⚡

## 📊 Cache İstatistikleri

```bash
curl http://localhost:8083/ProlizWebServices/api/cache-management/statistics
```

## 🔧 Opsiyonel: Redis Kurulumu (Daha Hızlı!)

### Windows
[Redis for Windows](https://github.com/microsoftarchive/redis/releases) indir ve kur

```cmd
redis-server
```

### Linux
```bash
sudo apt install redis-server
sudo systemctl start redis
```

### macOS
```bash
brew install redis
brew services start redis
```

Redis ile birlikte çalıştığında cache yanıt süresi **~1ms**'ye düşer! 🚀

## 📚 Detaylı Dokümantasyon

- **MySQL Kurulum:** [MYSQL_SETUP.md](MYSQL_SETUP.md)
- **Tam Kurulum:** [INSTALLATION.md](INSTALLATION.md)
- **Cache Mimarisi:** [CACHE_ARCHITECTURE.md](CACHE_ARCHITECTURE.md)
- **Ana README:** [README.md](README.md)

## 🐛 Sorun mu Yaşıyorsunuz?

### MariaDB Bağlantı Hatası

```bash
# MariaDB çalışıyor mu?
# Windows
sc query MariaDB

# Linux
sudo systemctl status mariadb

# Bağlantıyı test et
mysql -u proliz -p proliz_cache
```

### Port Çakışması

`application.properties` dosyasında portu değiştirin:
```properties
server.port=8084
```

### Şifre Hatası

Environment variable'ı doğru ayarladığınızdan emin olun:
```bash
# Windows
echo %DATABASE_PASSWORD%

# Linux/macOS
echo $DATABASE_PASSWORD
```

## 🎉 Başarılı!

Artık uygulamanız çalışıyor! 

- **Swagger UI:** http://localhost:8083/ProlizWebServices/swagger-ui.html
- **Cache Stats:** http://localhost:8083/ProlizWebServices/api/cache-management/statistics
- **Health:** http://localhost:8083/ProlizWebServices/api/cache-management/health

**Not:** İlk çalıştırmada tüm veriler SOAP'tan çekilir (~30-60 dakika). Sonraki çalıştırmalarda veriler cache'ten gelir (saniyeler içinde)! ⚡
