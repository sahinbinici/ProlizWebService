#!/bin/bash
# Tomcat Environment Variables for ProlizWebServices
# Bu dosyayı şuraya kopyalayın: /var/lib/tomcat9/bin/setenv.sh
# veya: /opt/tomcat/bin/setenv.sh

# Database Configuration
# MariaDB aynı sunucuda ise localhost kullanın
export DATABASE_URL="jdbc:mariadb://localhost:3306/proliz_cache?useSSL=false&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8"
export DATABASE_USERNAME="root"
export DATABASE_PASSWORD="sahinbey_"
export DATABASE_DRIVER="org.mariadb.jdbc.Driver"

# Redis Configuration
# Redis aynı sunucuda ise localhost kullanın
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD="sahinbey_"

# SOAP Service Credentials
export SOAP_SERVICE_USERNAME="ProLmsGan"
export SOAP_SERVICE_PASSWORD="-2020+Pro*Gan#"

# Cache Configuration
export CACHE_DISK_DIR="/opt/proliz/cache"
export LOG_FILE_PATH="/opt/proliz/logs/proliz-web-services.log"

# Cache Strategy
export CACHE_REDIS_ENABLED="true"
export CACHE_DISK_ENABLED="true"
export CACHE_DB_ENABLED="true"

# Logging Levels
export LOG_LEVEL_APP="INFO"
export LOG_LEVEL_CLIENT="INFO"
export LOG_LEVEL_WEB="WARN"

# JVM Options - Memory
export CATALINA_OPTS="$CATALINA_OPTS -Xmx4G"
export CATALINA_OPTS="$CATALINA_OPTS -Xms1G"

# JVM Options - Garbage Collection
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=200"
export CATALINA_OPTS="$CATALINA_OPTS -XX:ParallelGCThreads=4"
export CATALINA_OPTS="$CATALINA_OPTS -XX:ConcGCThreads=2"

# JVM Options - Heap Dump on OOM
export CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
export CATALINA_OPTS="$CATALINA_OPTS -XX:HeapDumpPath=/opt/proliz/logs"

# JVM Options - Performance
export CATALINA_OPTS="$CATALINA_OPTS -server"
export CATALINA_OPTS="$CATALINA_OPTS -Djava.security.egd=file:/dev/./urandom"

# JVM Options - Encoding
export CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF-8"

echo "ProlizWebServices Environment Variables Loaded"
echo "Database: localhost:3306/proliz_cache"
echo "Redis: localhost:6379"
echo "JVM Memory: Xms1G Xmx4G"
