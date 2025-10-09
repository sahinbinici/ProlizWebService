# Installation Guide - Hybrid Cache System

## 📋 Gereksinimler

### Zorunlu
- **Java 17** veya üzeri
- **Maven 3.6+**
- **MariaDB 10.5+** veya **MySQL 8.0+** (Veritabanı)
- **Disk alanı**: En az 2GB (cache + database için)

### Opsiyonel (Önerilen)
- **Redis 6.0+** (En iyi performans için)

**Not:** MariaDB önerilir (MySQL ile %100 uyumlu, daha hızlı)

## 🚀 Hızlı Kurulum

### 1. Projeyi İndir

```bash
git clone <repository-url>
cd ProlizWebServices
```

### 2. MariaDB Kurulumu ve Yapılandırması

**Detaylı kurulum için:** [MYSQL_SETUP.md](MYSQL_SETUP.md)

```bash
# MariaDB'ye bağlan (mysql komutu ile)
mysql -u root -p

# Veritabanı ve kullanıcı oluştur
CREATE DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'proliz'@'localhost' IDENTIFIED BY 'güçlü_şifre';
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'proliz'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Not:** MySQL kullanıyorsanız aynı komutlar çalışır.

### 3. Uygulama Yapılandırması

`application.properties` dosyasında MariaDB şifresini güncelleyin:

```properties
spring.datasource.password=güçlü_şifre
```

**Veya** environment variable kullanın (önerilen):

```bash
# Windows (CMD)
set DATABASE_PASSWORD=güçlü_şifre

# Windows (PowerShell)
$env:DATABASE_PASSWORD="güçlü_şifre"

# Linux/macOS
export DATABASE_PASSWORD=güçlü_şifre
```

### 4. Redis Başlat (Opsiyonel ama Önerilen)

```bash
# Windows - Redis installer ile kuruluysa
redis-server

# Linux
sudo systemctl start redis

# macOS
brew services start redis
```

Redis yoksa uygulama Disk Cache + Database ile çalışır.

### 5. Uygulamayı Çalıştır

```bash
mvn clean install
mvn spring-boot:run
```

### 6. Kontrol Et

- **Swagger UI**: http://localhost:8083/ProlizWebServices/swagger-ui.html
- **Cache Stats**: http://localhost:8083/ProlizWebServices/api/cache-management/statistics
- **Health Check**: http://localhost:8083/ProlizWebServices/api/cache-management/health

## 🏢 Production Kurulumu

### 1. PostgreSQL Kurulumu

```bash
# Docker ile
docker run -d \
  --name proliz-postgres \
  -e POSTGRES_DB=proliz_cache \
  -e POSTGRES_USER=proliz \
  -e POSTGRES_PASSWORD=your_secure_password \
  -p 5432:5432 \
  postgres:13
```

### 2. Redis Kurulumu

```bash
# Docker ile (persistence aktif)
docker run -d \
  --name proliz-redis \
  -p 6379:6379 \
  -v redis-data:/data \
  redis:latest redis-server --appendonly yes
```

### 3. Application Properties (Production)

`application-prod.properties` oluşturun:

```properties
# Server
server.port=8080

# Redis
spring.data.redis.host=your-redis-host
spring.data.redis.port=6379
spring.data.redis.password=your_redis_password

# PostgreSQL
spring.datasource.url=jdbc:postgresql://your-postgres-host:5432/proliz_cache
spring.datasource.username=proliz
spring.datasource.password=your_secure_password
spring.datasource.driverClassName=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Cache TTL (Production değerleri)
cache.ttl.soap-response=43200  # 12 saat
cache.ttl.ders-list=1800       # 30 dakika
cache.ttl.ogrenci-list=900     # 15 dakika

# H2 Console (Production'da kapalı)
spring.h2.console.enabled=false

# Logging
logging.level.com.prolizwebservices=INFO
```

### 4. Build ve Deploy

```bash
# WAR dosyası oluştur
mvn clean package -Pprod

# Çalıştır
java -jar target/ProlizWebServices-0.0.1-SNAPSHOT.war --spring.profiles.active=prod
```

## 🐳 Docker Compose ile Kurulum

`docker-compose.yml` oluşturun:

```yaml
version: '3.8'

services:
  redis:
    image: redis:latest
    container_name: proliz-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    restart: unless-stopped

  postgres:
    image: postgres:13
    container_name: proliz-postgres
    environment:
      POSTGRES_DB: proliz_cache
      POSTGRES_USER: proliz
      POSTGRES_PASSWORD: your_secure_password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped

  app:
    build: .
    container_name: proliz-app
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      REDIS_HOST: redis
      DATABASE_URL: jdbc:postgresql://postgres:5432/proliz_cache
      DATABASE_USERNAME: proliz
      DATABASE_PASSWORD: your_secure_password
    depends_on:
      - redis
      - postgres
    restart: unless-stopped

volumes:
  redis-data:
  postgres-data:
```

Çalıştır:

```bash
docker-compose up -d
```

## ⚙️ Konfigürasyon Seçenekleri

### Cache Stratejisi

```properties
# Cache katmanlarını aç/kapa
cache.strategy.redis.enabled=true
cache.strategy.disk.enabled=true
cache.strategy.database.enabled=true
```

### TTL Ayarları

```properties
# Saniye cinsinden
cache.ttl.soap-response=86400      # 24 saat
cache.ttl.ders-list=3600           # 1 saat
cache.ttl.ogrenci-list=1800        # 30 dakika
cache.ttl.ogretim-elemani=7200     # 2 saat
```

### Disk Cache

```properties
cache.disk.directory=./cache
cache.disk.max-size-mb=500
```

### Otomatik Refresh

```properties
cache.refresh.auto-enabled=true
cache.refresh.cron=0 0 2 * * ?     # Her gece 2:00
cache.refresh.on-startup=true
```

## 🔍 Kurulum Doğrulama

### 1. Health Check

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

### 2. Cache İstatistikleri

```bash
curl http://localhost:8083/ProlizWebServices/api/cache-management/statistics
```

### 3. Redis Bağlantısı

```bash
redis-cli ping
# Yanıt: PONG
```

### 4. Database Bağlantısı

H2 Console: http://localhost:8083/ProlizWebServices/h2-console

- **JDBC URL**: `jdbc:h2:file:./data/proliz_cache`
- **Username**: `sa`
- **Password**: (boş)

## 🛠️ Troubleshooting

### Redis Bağlanamıyor

```properties
# Redis'i devre dışı bırak
cache.strategy.redis.enabled=false
```

### Database Hatası

```bash
# Database dosyalarını sıfırla
rm -rf ./data/proliz_cache.*
```

### Disk Cache Temizliği

```bash
# Cache klasörünü temizle
rm -rf ./cache/*
```

### Port Çakışması

```properties
# Farklı port kullan
server.port=8084
```

### Memory Hatası

```bash
# JVM heap size artır
java -Xmx2G -Xms512M -jar target/ProlizWebServices-0.0.1-SNAPSHOT.war
```

## 📊 İlk Veri Yükleme

İlk çalıştırmada cache boş olacaktır. Veri yüklemek için:

### 1. Manuel Yükleme

```bash
# Ders listesini çek (ilk kez SOAP'tan gelir)
curl http://localhost:8083/ProlizWebServices/api/data/dersler?page=0&size=20
```

### 2. Otomatik Yükleme

Uygulama başladığında `DataCacheService` otomatik olarak:
- Ders listesini yükler
- Öğretim elemanlarını yükler
- İlk 100 dersin öğrencilerini yükler
- Geri kalanı arka planda yükler

### 3. İlerlemeyi Takip Et

```bash
# Progressive loading durumu
curl http://localhost:8083/ProlizWebServices/api/data/cache/progressive-status
```

## 🔐 Güvenlik Önerileri

### Production'da Mutlaka Yapın

1. **Redis Şifresi Ayarla**
   ```properties
   spring.data.redis.password=strong_password_here
   ```

2. **Database Şifresi Değiştir**
   ```properties
   spring.datasource.password=strong_password_here
   ```

3. **H2 Console'u Kapat**
   ```properties
   spring.h2.console.enabled=false
   ```

4. **SOAP Credentials'ı Çevre Değişkenine Taşı**
   ```bash
   export SOAP_SERVICE_USERNAME=your_username
   export SOAP_SERVICE_PASSWORD=your_password
   ```

5. **HTTPS Aktif Et**
   ```properties
   server.ssl.enabled=true
   server.ssl.key-store=classpath:keystore.p12
   server.ssl.key-store-password=your_keystore_password
   ```

## 📈 Performans Optimizasyonu

### Redis için

```properties
spring.data.redis.jedis.pool.max-active=50
spring.data.redis.jedis.pool.max-idle=20
spring.data.redis.jedis.pool.min-idle=10
```

### Database için

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.fetch_size=50
```

### JVM için

```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xmx4G \
     -Xms1G \
     -jar target/ProlizWebServices-0.0.1-SNAPSHOT.war
```

## 📞 Destek

Sorun yaşarsanız:
1. Logları kontrol edin: `logs/proliz-web-services.log`
2. Health endpoint'i kontrol edin
3. GitHub Issues'a bildirin

## ✅ Kurulum Checklist

- [ ] Java 17 kurulu
- [ ] Maven kurulu
- [ ] Redis çalışıyor (opsiyonel)
- [ ] PostgreSQL çalışıyor (production için)
- [ ] application.properties yapılandırıldı
- [ ] Uygulama başarıyla başladı
- [ ] Health check PASSED
- [ ] Swagger UI erişilebilir
- [ ] İlk cache yükleme tamamlandı
- [ ] Metrikler çalışıyor

Tebrikler! 🎉 Hybrid cache sistemi başarıyla kuruldu.
