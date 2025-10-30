# Tomcat Deployment Rehberi - 193.140.136.26

## 🎯 Tomcat'e WAR Deployment

### Ön Hazırlık

#### 1. Gerekli Servisler
```bash
# MariaDB kurulumu ve başlatma
sudo apt update
sudo apt install mariadb-server -y
sudo systemctl start mariadb
sudo systemctl enable mariadb

# Redis kurulumu (opsiyonel)
sudo apt install redis-server -y
sudo systemctl start redis
sudo systemctl enable redis
```

#### 2. Veritabanı Oluşturma
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

---

## 📦 WAR Dosyası Hazırlama

### 1. Projeyi Build Et
```bash
cd /path/to/ProlizWebServices

# Clean build
mvn clean package -DskipTests

# WAR dosyası oluşturuldu:
# target/ProlizWebServices-0.0.1-SNAPSHOT.war
```

### 2. WAR Dosyasını Yeniden Adlandır (Opsiyonel)
```bash
# Context path için daha kısa isim
cd target
cp ProlizWebServices-0.0.1-SNAPSHOT.war ProlizWebServices.war
```

---

## 🚀 Tomcat'e Deployment

### Yöntem 1: Manuel Deployment

#### 1. WAR Dosyasını Tomcat'e Kopyala
```bash
# Yerel bilgisayardan sunucuya
scp target/ProlizWebServices.war user@193.140.136.26:/tmp/

# Sunucuda Tomcat webapps klasörüne kopyala
ssh user@193.140.136.26
sudo cp /tmp/ProlizWebServices.war /var/lib/tomcat9/webapps/

# Veya doğrudan
sudo cp /tmp/ProlizWebServices.war /opt/tomcat/webapps/
```

#### 2. Tomcat'i Yeniden Başlat
```bash
# Ubuntu/Debian
sudo systemctl restart tomcat9

# Manuel Tomcat kurulumu
cd /opt/tomcat/bin
./shutdown.sh
./startup.sh
```

#### 3. Deployment Loglarını İzle
```bash
# Tomcat logs
tail -f /var/lib/tomcat9/logs/catalina.out

# Veya
tail -f /opt/tomcat/logs/catalina.out
```

---

### Yöntem 2: Tomcat Manager UI

1. **Tomcat Manager'a Erişim**
   - URL: `http://193.140.136.26:8080/manager/html`
   - Kullanıcı adı ve şifre gerekli

2. **WAR Dosyasını Upload Et**
   - "WAR file to deploy" bölümünden dosya seç
   - "Deploy" butonuna tıkla

3. **Deployment Durumunu Kontrol Et**
   - Applications listesinde `/ProlizWebServices` görünmeli
   - Status: Running olmalı

---

## ⚙️ Tomcat Konfigürasyonu

### 1. Environment Variables Ayarlama

**Tomcat setenv.sh oluştur:**
```bash
sudo nano /var/lib/tomcat9/bin/setenv.sh
# Veya
sudo nano /opt/tomcat/bin/setenv.sh
```

**setenv.sh içeriği:**
```bash
#!/bin/bash

# Database Configuration
export DATABASE_URL="jdbc:mariadb://193.140.136.26:3306/proliz_cache?useSSL=false&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8"
export DATABASE_USERNAME="root"
export DATABASE_PASSWORD="sahinbey_"
export DATABASE_DRIVER="org.mariadb.jdbc.Driver"

# Redis Configuration
export REDIS_HOST="193.140.136.26"
export REDIS_PORT="6379"
export REDIS_PASSWORD="sahinbey_"

# SOAP Service Credentials
export SOAP_SERVICE_USERNAME="ProLmsGan"
export SOAP_SERVICE_PASSWORD="-2020+Pro*Gan#"

# Cache Configuration
export CACHE_DISK_DIR="/opt/proliz/cache"
export LOG_FILE_PATH="/opt/proliz/logs/proliz-web-services.log"

# JVM Options
export CATALINA_OPTS="$CATALINA_OPTS -Xmx4G -Xms1G"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=200"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
export CATALINA_OPTS="$CATALINA_OPTS -XX:HeapDumpPath=/opt/proliz/logs"

# Logging
export CATALINA_OPTS="$CATALINA_OPTS -Dlogging.level.com.prolizwebservices=INFO"
```

**Çalıştırılabilir yap:**
```bash
sudo chmod +x /var/lib/tomcat9/bin/setenv.sh
```

### 2. Gerekli Dizinleri Oluştur
```bash
sudo mkdir -p /opt/proliz/{cache,logs,data}
sudo chown -R tomcat:tomcat /opt/proliz

# Veya Tomcat kullanıcınıza göre
sudo chown -R tomcat9:tomcat9 /opt/proliz
```

### 3. Tomcat server.xml Ayarları (Opsiyonel)

**Port ve Connector ayarları:**
```bash
sudo nano /var/lib/tomcat9/conf/server.xml
```

```xml
<!-- HTTP Connector -->
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"
           maxThreads="200"
           minSpareThreads="25"
           maxConnections="10000"
           acceptCount="100"
           compression="on"
           compressionMinSize="2048"
           URIEncoding="UTF-8" />
```

---

## 🔍 Deployment Doğrulama

### 1. Uygulama Çalışıyor mu?
```bash
# Tomcat üzerinde çalışan uygulamalar
curl http://193.140.136.26:8080/manager/text/list -u admin:password

# Beklenen çıktı:
# OK - Listed applications for virtual host [localhost]
# /ProlizWebServices:running:0:ProlizWebServices
```

### 2. Health Check
```bash
# Tomcat portu genelde 8080
curl http://193.140.136.26:8080/ProlizWebServices/api/cache-management/health

# Beklenen yanıt:
{
  "status": "UP",
  "cacheEnabled": true,
  "healthScore": 100.0
}
```

### 3. Swagger UI Erişimi
Tarayıcıda açın:
```
http://193.140.136.26:8080/ProlizWebServices/swagger-ui.html
```

### 4. Root Endpoint
```bash
curl http://193.140.136.26:8080/ProlizWebServices/health

# Veya
curl http://193.140.136.26:8080/ProlizWebServices/
```

---

## 🚨 CORS Hatası Çözümü

### Sorun
Swagger UI'da "Failed to fetch" hatası alıyorsanız:

### Çözüm 1: CORS Ayarları Güncellendi ✅
Tüm controller'larda CORS ayarları şu şekilde güncellendi:
```java
@CrossOrigin(
    origins = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
    allowedHeaders = "*",
    allowCredentials = "false",
    maxAge = 3600
)
```

### Çözüm 2: Tomcat CORS Filter (Alternatif)

**web.xml oluştur (src/main/webapp/WEB-INF/web.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

    <!-- CORS Filter -->
    <filter>
        <filter-name>CorsFilter</filter-name>
        <filter-class>org.apache.catalina.filters.CorsFilter</filter-class>
        <init-param>
            <param-name>cors.allowed.origins</param-name>
            <param-value>*</param-value>
        </init-param>
        <init-param>
            <param-name>cors.allowed.methods</param-name>
            <param-value>GET,POST,PUT,DELETE,OPTIONS,HEAD</param-value>
        </init-param>
        <init-param>
            <param-name>cors.allowed.headers</param-name>
            <param-value>*</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>CorsFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

</web-app>
```

---

## 📊 Tomcat İzleme ve Yönetim

### 1. Tomcat Manager Kullanıcısı Oluştur
```bash
sudo nano /var/lib/tomcat9/conf/tomcat-users.xml
```

```xml
<tomcat-users>
  <role rolename="manager-gui"/>
  <role rolename="manager-script"/>
  <role rolename="admin-gui"/>
  <user username="admin" password="güçlü_şifre" roles="manager-gui,manager-script,admin-gui"/>
</tomcat-users>
```

### 2. Manager Context Ayarları
```bash
sudo nano /var/lib/tomcat9/webapps/manager/META-INF/context.xml
```

**IP kısıtlamasını kaldır:**
```xml
<Context antiResourceLocking="false" privileged="true" >
  <!-- Valve'i yorum satırına al veya IP ekle -->
  <!--
  <Valve className="org.apache.catalina.valves.RemoteAddrValve"
         allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1|193\.140\.136\.26" />
  -->
</Context>
```

### 3. Uygulama Yönetimi
```bash
# Uygulamayı durdur
curl "http://193.140.136.26:8080/manager/text/stop?path=/ProlizWebServices" -u admin:password

# Uygulamayı başlat
curl "http://193.140.136.26:8080/manager/text/start?path=/ProlizWebServices" -u admin:password

# Uygulamayı yeniden yükle
curl "http://193.140.136.26:8080/manager/text/reload?path=/ProlizWebServices" -u admin:password

# Uygulamayı kaldır
curl "http://193.140.136.26:8080/manager/text/undeploy?path=/ProlizWebServices" -u admin:password
```

---

## 🔧 Sorun Giderme

### 1. Uygulama Başlamıyor

**Logları kontrol et:**
```bash
# Tomcat logs
tail -f /var/lib/tomcat9/logs/catalina.out

# Uygulama logs
tail -f /opt/proliz/logs/proliz-web-services.log

# Tomcat localhost log
tail -f /var/lib/tomcat9/logs/localhost.*.log
```

**Yaygın hatalar:**
- **Database connection refused**: MariaDB çalışıyor mu? `sudo systemctl status mariadb`
- **Redis connection timeout**: Redis çalışıyor mu? `sudo systemctl status redis`
- **Port already in use**: Başka bir uygulama 8080 portunu kullanıyor mu?

### 2. CORS Hatası Devam Ediyor

**Tarayıcı konsolunu kontrol et:**
```javascript
// Chrome DevTools > Console
// Hatayı göreceksiniz
```

**Çözüm:**
```bash
# 1. Yeniden build et
mvn clean package -DskipTests

# 2. Eski WAR'ı sil
sudo rm -rf /var/lib/tomcat9/webapps/ProlizWebServices*

# 3. Yeni WAR'ı kopyala
sudo cp target/ProlizWebServices.war /var/lib/tomcat9/webapps/

# 4. Tomcat'i yeniden başlat
sudo systemctl restart tomcat9

# 5. Deployment'i bekle (30-60 saniye)
tail -f /var/lib/tomcat9/logs/catalina.out
```

### 3. Memory Hatası (OutOfMemoryError)

**setenv.sh'de heap size artır:**
```bash
export CATALINA_OPTS="$CATALINA_OPTS -Xmx8G -Xms2G"
```

### 4. Slow Startup

**Entropy sorununu çöz:**
```bash
# setenv.sh'e ekle
export CATALINA_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/./urandom"
```

---

## 📈 Performans Optimizasyonu

### 1. Tomcat Thread Pool
```xml
<!-- server.xml -->
<Executor name="tomcatThreadPool" 
          namePrefix="catalina-exec-"
          maxThreads="200" 
          minSpareThreads="25"
          maxIdleTime="60000"/>

<Connector executor="tomcatThreadPool"
           port="8080" 
           protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
```

### 2. JVM Tuning
```bash
# setenv.sh
export CATALINA_OPTS="$CATALINA_OPTS -server"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=200"
export CATALINA_OPTS="$CATALINA_OPTS -XX:ParallelGCThreads=4"
export CATALINA_OPTS="$CATALINA_OPTS -XX:ConcGCThreads=2"
export CATALINA_OPTS="$CATALINA_OPTS -XX:InitiatingHeapOccupancyPercent=45"
```

### 3. Compression
```xml
<!-- server.xml -->
<Connector port="8080" protocol="HTTP/1.1"
           compression="on"
           compressionMinSize="2048"
           compressibleMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json" />
```

---

## 🔄 Güncelleme Prosedürü

### 1. Zero-Downtime Deployment (Paralel Deployment)

**WAR dosyasını versiyonla:**
```bash
# Yeni versiyon
cp target/ProlizWebServices.war target/ProlizWebServices##v2.war

# Sunucuya kopyala
scp target/ProlizWebServices##v2.war user@193.140.136.26:/var/lib/tomcat9/webapps/
```

Tomcat otomatik olarak yeni versiyonu deploy eder:
- Eski: `http://193.140.136.26:8080/ProlizWebServices`
- Yeni: `http://193.140.136.26:8080/ProlizWebServices##v2`

Test ettikten sonra eski versiyonu kaldır.

### 2. Standard Update
```bash
# 1. Uygulamayı durdur
curl "http://193.140.136.26:8080/manager/text/stop?path=/ProlizWebServices" -u admin:password

# 2. Eski WAR'ı sil
sudo rm -rf /var/lib/tomcat9/webapps/ProlizWebServices*

# 3. Yeni WAR'ı kopyala
sudo cp /tmp/ProlizWebServices.war /var/lib/tomcat9/webapps/

# 4. Tomcat'i yeniden başlat
sudo systemctl restart tomcat9
```

---

## ✅ Deployment Checklist

- [ ] Java 17 kurulu
- [ ] Tomcat 9+ kurulu ve çalışıyor
- [ ] MariaDB kurulu ve yapılandırılmış
- [ ] Redis kurulu (opsiyonel)
- [ ] Veritabanı oluşturuldu (proliz_cache)
- [ ] Dizinler oluşturuldu (/opt/proliz/*)
- [ ] setenv.sh oluşturuldu ve environment variables ayarlandı
- [ ] WAR dosyası build edildi
- [ ] WAR dosyası Tomcat webapps'e kopyalandı
- [ ] Tomcat yeniden başlatıldı
- [ ] Deployment logları kontrol edildi
- [ ] Health check PASSED
- [ ] Swagger UI erişilebilir
- [ ] CORS çalışıyor
- [ ] İlk cache yükleme başladı

---

## 📞 Erişim Bilgileri (Tomcat)

### Varsayılan Tomcat Portu: 8080

- **Base URL**: `http://193.140.136.26:8080/ProlizWebServices`
- **Swagger UI**: `http://193.140.136.26:8080/ProlizWebServices/swagger-ui.html`
- **Health Check**: `http://193.140.136.26:8080/ProlizWebServices/api/cache-management/health`
- **Tomcat Manager**: `http://193.140.136.26:8080/manager/html`

### Test Komutları
```bash
# Health check
curl http://193.140.136.26:8080/ProlizWebServices/api/cache-management/health

# Ders listesi
curl "http://193.140.136.26:8080/ProlizWebServices/api/data/dersler?page=0&size=10"

# Cache istatistikleri
curl http://193.140.136.26:8080/ProlizWebServices/api/cache-management/statistics
```

---

## 🎯 Sonraki Adımlar

1. **Yeniden Build Et** - CORS düzeltmeleri ile
2. **WAR'ı Deploy Et** - Tomcat'e kopyala
3. **Test Et** - Swagger UI'da dene
4. **Monitor Et** - Logları takip et

**Şimdi yapmanız gerekenler:**
```bash
# 1. Projeyi yeniden build et
mvn clean package -DskipTests

# 2. WAR'ı sunucuya kopyala
scp target/ProlizWebServices.war user@193.140.136.26:/tmp/

# 3. Sunucuda deploy et
ssh user@193.140.136.26
sudo rm -rf /var/lib/tomcat9/webapps/ProlizWebServices*
sudo cp /tmp/ProlizWebServices.war /var/lib/tomcat9/webapps/
sudo systemctl restart tomcat9

# 4. Logları izle
tail -f /var/lib/tomcat9/logs/catalina.out
```

Deployment başarılı! 🎉
