# 🗄️ Veritabanı Bilgileri

## MariaDB vs MySQL

### Neden MariaDB?

**MariaDB**, MySQL'in orijinal geliştiricisi tarafından oluşturulan, açık kaynaklı bir fork'tur.

#### Avantajlar

1. **%100 MySQL Uyumlu**
   - Tüm MySQL komutları çalışır
   - `mysql` komutu ile bağlanılır
   - Kod değişikliği gerektirmez

2. **Daha Hızlı**
   - Optimize edilmiş query engine
   - Daha iyi performans
   - Daha az memory kullanımı

3. **Açık Kaynak**
   - Tamamen GPL lisanslı
   - Community-driven
   - Oracle'a bağımlı değil

4. **Modern Özellikler**
   - JSON desteği
   - Window functions
   - Common Table Expressions (CTE)

### Proje Konfigürasyonu

#### MariaDB (Varsayılan)

```properties
# application.properties
spring.datasource.url=jdbc:mariadb://localhost:3306/proliz_cache...
spring.datasource.driverClassName=org.mariadb.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
```

#### MySQL (Alternatif)

```properties
# application.properties (uncomment)
spring.datasource.url=jdbc:mysql://localhost:3306/proliz_cache...
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### Dependency'ler

```xml
<!-- pom.xml -->

<!-- MariaDB Driver (default) -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
</dependency>

<!-- MySQL Driver (optional) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <optional>true</optional>
</dependency>
```

## Kurulum

### MariaDB Kurulumu

#### Windows
```bash
# https://mariadb.org/download/ adresinden indirin
# MSI installer'ı çalıştırın
net start MariaDB
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mariadb-server
sudo systemctl start mariadb
sudo mysql_secure_installation
```

#### macOS
```bash
brew install mariadb
brew services start mariadb
mysql_secure_installation
```

### MySQL Kurulumu (Alternatif)

#### Windows
```bash
# https://dev.mysql.com/downloads/mysql/ adresinden indirin
net start MySQL80
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

#### macOS
```bash
brew install mysql
brew services start mysql
```

## Veritabanı Oluşturma

**Her iki veritabanı için aynı:**

```sql
-- Bağlan
mysql -u root -p

-- Veritabanı oluştur
CREATE DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Kullanıcı oluştur
CREATE USER 'proliz'@'localhost' IDENTIFIED BY 'güçlü_şifre';

-- Yetkilendir
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'proliz'@'localhost';
FLUSH PRIVILEGES;

EXIT;
```

## Geçiş (Migration)

### MySQL'den MariaDB'ye

```bash
# 1. Veriyi yedekle
mysqldump -u root -p proliz_cache > backup.sql

# 2. MariaDB'yi kur
# (Yukarıdaki kurulum adımlarını takip edin)

# 3. Veriyi geri yükle
mysql -u root -p proliz_cache < backup.sql

# 4. application.properties'i güncelle
# MariaDB konfigürasyonunu kullan
```

### MariaDB'den MySQL'e

```bash
# 1. Veriyi yedekle
mysqldump -u root -p proliz_cache > backup.sql

# 2. MySQL'i kur
# (Yukarıdaki kurulum adımlarını takip edin)

# 3. Veriyi geri yükle
mysql -u root -p proliz_cache < backup.sql

# 4. application.properties'i güncelle
# MySQL konfigürasyonunu uncomment et
```

## Performans Karşılaştırması

| Özellik | MariaDB | MySQL |
|---------|---------|-------|
| **Hız** | ⚡⚡⚡ Daha hızlı | ⚡⚡ Hızlı |
| **Memory** | 💾 Daha az | 💾💾 Daha fazla |
| **Lisans** | ✅ Tamamen açık | ⚠️ Dual license |
| **Uyumluluk** | ✅ %100 MySQL | ✅ Native |
| **Community** | 👥 Aktif | 👥 Büyük |

## Önerilen Seçim

### Development
- **MariaDB** veya **H2** (in-memory)

### Production
- **MariaDB** (önerilen)
- **MySQL** (alternatif)

### Neden MariaDB?
1. ✅ Daha hızlı
2. ✅ Daha az kaynak tüketimi
3. ✅ Tamamen açık kaynak
4. ✅ MySQL ile %100 uyumlu
5. ✅ Modern özellikler

## Troubleshooting

### Port Çakışması (3306)

**MariaDB:**
```bash
# my.cnf veya my.ini
[mysqld]
port=3307
```

**MySQL:**
```bash
# my.cnf veya my.ini
[mysqld]
port=3307
```

**Application:**
```properties
spring.datasource.url=jdbc:mariadb://localhost:3307/proliz_cache...
```

### Karakter Seti Sorunu

```sql
-- Veritabanı karakter setini kontrol et
SHOW CREATE DATABASE proliz_cache;

-- Değiştir
ALTER DATABASE proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Bağlantı Sorunu

```bash
# Servis çalışıyor mu?
# MariaDB
sudo systemctl status mariadb

# MySQL
sudo systemctl status mysql

# Bağlantıyı test et
mysql -u proliz -p proliz_cache
```

## Sonuç

**Proje varsayılan olarak MariaDB kullanır** ancak MySQL ile de %100 uyumludur. 

**Değiştirmek için:**
1. `application.properties` dosyasında ilgili satırları uncomment edin
2. Uygulamayı yeniden başlatın

**Hiçbir kod değişikliği gerekmez!** 🎉

## Detaylı Dokümantasyon

- **Kurulum Rehberi**: [MYSQL_SETUP.md](MYSQL_SETUP.md)
- **Hızlı Başlangıç**: [QUICK_START.md](QUICK_START.md)
- **Tam Kurulum**: [INSTALLATION.md](INSTALLATION.md)
