# 🚨 MariaDB Bağlantı Hatası - Hızlı Çözüm

## Sorun
```
Host 'hesap.gaziantep.edu.tr' is not allowed to connect to this MariaDB server
```

---

## ✅ Çözüm Adımları

### 1. MariaDB Kullanıcı Yetkilerini Düzelt

Sunucuda şu komutları çalıştırın:

```bash
# MariaDB'ye root olarak bağlan
sudo mysql -u root -p
# Şifre: sahinbey_
```

MariaDB içinde:

```sql
-- Tüm host'lardan root erişimine izin ver
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'root'@'%' IDENTIFIED BY 'sahinbey_';

-- Localhost için de ekle
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'root'@'localhost' IDENTIFIED BY 'sahinbey_';

-- Hesap hostname için de ekle
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'root'@'hesap.gaziantep.edu.tr' IDENTIFIED BY 'sahinbey_';

-- Değişiklikleri uygula
FLUSH PRIVILEGES;

-- Kontrol et
SELECT User, Host FROM mysql.user WHERE User='root';

-- Çıkış
EXIT;
```

### 2. MariaDB Bind Address Kontrol

```bash
# MariaDB konfigürasyonunu kontrol et
sudo nano /etc/mysql/mariadb.conf.d/50-server.cnf
```

**Değiştir:**
```ini
# Bu satırı bul:
bind-address = 127.0.0.1

# Şuna değiştir (tüm interface'lerden dinle):
bind-address = 0.0.0.0
```

**Kaydet ve çık:** `Ctrl+X`, `Y`, `Enter`

**MariaDB'yi yeniden başlat:**
```bash
sudo systemctl restart mariadb
sudo systemctl status mariadb
```

### 3. Firewall Kontrol (Gerekirse)

```bash
# MariaDB portunu aç (sadece gerekirse)
sudo ufw allow 3306/tcp
```

---

## 🔄 Uygulamayı Yeniden Deploy Et

### Adım 1: Yeni WAR Build Et
```bash
# Windows'ta
cd C:\Users\cdikici\IdeaProjects\ProlizWebServices
mvn clean package -DskipTests
```

### Adım 2: setenv.sh Dosyasını Kopyala
```bash
# Windows'tan sunucuya
scp setenv.sh user@193.140.136.26:/tmp/

# Sunucuda
ssh user@193.140.136.26
sudo cp /tmp/setenv.sh /var/lib/tomcat9/bin/setenv.sh
sudo chmod +x /var/lib/tomcat9/bin/setenv.sh
sudo chown tomcat:tomcat /var/lib/tomcat9/bin/setenv.sh
```

### Adım 3: WAR Dosyasını Deploy Et
```bash
# WAR'ı sunucuya kopyala
scp target/ProlizWebServices-0.0.1-SNAPSHOT.war user@193.140.136.26:/tmp/

# Sunucuda deploy et
ssh user@193.140.136.26

# Eski deployment'ı temizle
sudo rm -rf /var/lib/tomcat9/webapps/ProlizWebServices*

# Yeni WAR'ı kopyala
sudo cp /tmp/ProlizWebServices-0.0.1-SNAPSHOT.war /var/lib/tomcat9/webapps/ProlizWebServices.war

# Gerekli dizinleri oluştur
sudo mkdir -p /opt/proliz/{cache,logs,data}
sudo chown -R tomcat:tomcat /opt/proliz

# Tomcat'i yeniden başlat
sudo systemctl restart tomcat9

# Logları izle
tail -f /var/lib/tomcat9/logs/catalina.out
```

---

## 🧪 Test Et

### 1. MariaDB Bağlantısını Test Et
```bash
# Sunucuda
mysql -u root -p -h localhost proliz_cache
# Şifre: sahinbey_

# Başarılı bağlantı sonrası:
SHOW TABLES;
EXIT;
```

### 2. Redis Bağlantısını Test Et
```bash
redis-cli -a sahinbey_ ping
# Yanıt: PONG
```

### 3. Uygulama Health Check
```bash
# 30-60 saniye bekle (deployment için)
sleep 60

# Health check
curl http://193.140.136.26:8080/ProlizWebServices/api/cache-management/health
```

**Beklenen Yanıt:**
```json
{
  "status": "UP",
  "cacheEnabled": true,
  "healthScore": 100.0
}
```

### 4. Swagger UI
Tarayıcıda açın:
```
http://193.140.136.26:8080/ProlizWebServices/swagger-ui.html
```

---

## 📊 Sorun Devam Ederse

### Logları Kontrol Et

**Tomcat Logs:**
```bash
tail -f /var/lib/tomcat9/logs/catalina.out
tail -f /var/lib/tomcat9/logs/localhost.*.log
```

**Uygulama Logs:**
```bash
tail -f /opt/proliz/logs/proliz-web-services.log
```

**MariaDB Logs:**
```bash
sudo tail -f /var/log/mysql/error.log
```

### MariaDB Kullanıcılarını Kontrol Et
```bash
sudo mysql -u root -p
```

```sql
-- Tüm kullanıcıları listele
SELECT User, Host, plugin FROM mysql.user;

-- Root kullanıcısının yetkilerini kontrol et
SHOW GRANTS FOR 'root'@'localhost';
SHOW GRANTS FOR 'root'@'%';
```

### Bağlantı Testleri
```bash
# MariaDB dinliyor mu?
sudo netstat -tulpn | grep 3306

# Redis dinliyor mu?
sudo netstat -tulpn | grep 6379

# Tomcat çalışıyor mu?
sudo systemctl status tomcat9
```

---

## 🎯 Özet

**Yapılan Değişiklikler:**
1. ✅ `application.properties` - localhost kullanımı
2. ✅ `setenv.sh` - Tomcat environment variables
3. ✅ MariaDB yetkilendirme düzeltmesi
4. ✅ CORS ayarları düzeltildi

**Deployment Sırası:**
1. MariaDB yetkilerini düzelt
2. setenv.sh'i kopyala
3. Yeni WAR'ı deploy et
4. Test et

**Erişim:**
- Base URL: `http://193.140.136.26:8080/ProlizWebServices`
- Swagger: `http://193.140.136.26:8080/ProlizWebServices/swagger-ui.html`
- Health: `http://193.140.136.26:8080/ProlizWebServices/api/cache-management/health`

---

## 💡 Notlar

- MariaDB ve Redis aynı sunucuda ise **localhost** kullanın
- Environment variables için **setenv.sh** kullanın
- Tomcat kullanıcısına **/opt/proliz** dizinlerine yazma yetkisi verin
- İlk deployment 30-60 saniye sürebilir
- İlk cache yükleme 30-60 dakika sürebilir

Başarılar! 🚀
