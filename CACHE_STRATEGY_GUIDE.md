# 🎯 Cache Stratejisi Yönetim Rehberi

## 📋 Genel Bakış

Uygulama **dinamik cache stratejisi** desteği ile gelir. İstediğiniz zaman cache katmanlarını açıp kapatabilirsiniz:

- **Redis** (L1) - Memory cache
- **Disk** (L2) - File-based cache  
- **Database** (L3) - MySQL persistent storage

## 🚀 Hazır Stratejiler

### 1. **MEMORY_ONLY** (Sadece Redis)

**Ne Zaman Kullanılır:**
- En hızlı performans gerektiğinde
- Restart sonrası veri kaybı sorun değilse
- Development/Testing ortamında

**Özellikler:**
- ✅ En hızlı (~1ms)
- ❌ Restart'ta kaybolur
- ❌ Disk/DB kullanmaz

**Konfigürasyon:**
```properties
cache.strategy.redis.enabled=true
cache.strategy.disk.enabled=false
cache.strategy.database.enabled=false
```

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"
```

---

### 2. **FULL_PERSISTENCE** (Redis + Disk + Database)

**Ne Zaman Kullanılır:**
- Production ortamında
- Veri kaybı kabul edilemezse
- Maximum güvenilirlik gerektiğinde

**Özellikler:**
- ✅ Restart-safe
- ✅ 3-layer redundancy
- ✅ Maximum güvenilirlik
- ⚠️ Biraz daha yavaş yazma

**Konfigürasyon:**
```properties
cache.strategy.redis.enabled=true
cache.strategy.disk.enabled=true
cache.strategy.database.enabled=true
```

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=FULL_PERSISTENCE"
```

---

### 3. **DISK_AND_DATABASE** (Disk + Database, Redis Yok)

**Ne Zaman Kullanılır:**
- Redis kurulu değilse
- Memory sınırlıysa
- Persistent storage yeterli

**Özellikler:**
- ✅ Restart-safe
- ✅ Redis gerektirmez
- ⚠️ Biraz daha yavaş (~10-50ms)

**Konfigürasyon:**
```properties
cache.strategy.redis.enabled=false
cache.strategy.disk.enabled=true
cache.strategy.database.enabled=true
```

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=DISK_AND_DATABASE"
```

---

### 4. **REDIS_AND_DATABASE** (Redis + Database, Disk Yok)

**Ne Zaman Kullanılır:**
- Disk I/O minimize edilmek istendiğinde
- SSD yok, HDD yavaşsa

**Özellikler:**
- ✅ Hızlı (Redis)
- ✅ Persistent (Database)
- ❌ Disk kullanmaz

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=REDIS_AND_DATABASE"
```

---

### 5. **REDIS_ONLY** (Sadece Redis)

**Ne Zaman Kullanılır:**
- Extreme performance
- Temporary caching
- Development

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=REDIS_ONLY"
```

---

### 6. **DISK_ONLY** (Sadece Disk)

**Ne Zaman Kullanılır:**
- Redis ve Database yok
- Minimal setup

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=DISK_ONLY"
```

---

### 7. **DATABASE_ONLY** (Sadece Database)

**Ne Zaman Kullanılır:**
- Sadece MySQL var
- Centralized storage

**API Çağrısı:**
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=DATABASE_ONLY"
```

---

## 🔧 Runtime Yönetimi

### Mevcut Stratejiyi Görüntüle

```bash
curl http://localhost:8083/ProlizWebServices/api/cache-management/strategy
```

**Yanıt:**
```json
{
  "strategyName": "FULL_PERSISTENCE",
  "redisEnabled": true,
  "diskEnabled": true,
  "databaseEnabled": true,
  "timestamp": "2025-10-07T09:46:00"
}
```

---

### Strateji Değiştir

```bash
# Memory only'e geç
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"

# Full persistence'a geç
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=FULL_PERSISTENCE"
```

---

### Tek Bir Katmanı Aç/Kapat

#### Redis'i Kapat
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/redis?enabled=false"
```

#### Disk'i Aç
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/disk?enabled=true"
```

#### Database'i Kapat
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/database?enabled=false"
```

---

### Özel Kombinasyon

```bash
# Sadece Redis + Database
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/custom?redis=true&disk=false&database=true"

# Sadece Disk + Database
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/custom?redis=false&disk=true&database=true"
```

---

## 📊 Performans Karşılaştırması

| Strateji | Okuma Hızı | Yazma Hızı | Restart-Safe | Disk Kullanımı | Memory Kullanımı |
|----------|------------|------------|--------------|----------------|------------------|
| **MEMORY_ONLY** | ~1ms | ~1ms | ❌ | Yok | Yüksek |
| **FULL_PERSISTENCE** | ~1-10ms | ~50ms | ✅ | Orta | Orta |
| **DISK_AND_DATABASE** | ~10-50ms | ~50ms | ✅ | Yüksek | Düşük |
| **REDIS_AND_DATABASE** | ~1ms | ~50ms | ✅ | Yok | Yüksek |
| **REDIS_ONLY** | ~1ms | ~1ms | ❌ | Yok | Yüksek |
| **DISK_ONLY** | ~10ms | ~10ms | ✅ | Yüksek | Düşük |
| **DATABASE_ONLY** | ~50ms | ~50ms | ✅ | Düşük | Düşük |

---

## 🎯 Kullanım Senaryoları

### Senaryo 1: Development

**Hedef:** Hızlı geliştirme, kolay debug

**Strateji:** `MEMORY_ONLY` veya `REDIS_ONLY`

```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"
```

---

### Senaryo 2: Production (Yüksek Trafik)

**Hedef:** Maximum performans + güvenilirlik

**Strateji:** `FULL_PERSISTENCE`

```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=FULL_PERSISTENCE"
```

---

### Senaryo 3: Redis Çöktü

**Hedef:** Fallback to disk + database

**Strateji:** `DISK_AND_DATABASE`

```bash
# Redis'i kapat
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/redis?enabled=false"
```

Sistem otomatik olarak Disk ve Database'e geçer!

---

### Senaryo 4: Disk Doldu

**Hedef:** Disk kullanımını durdur

**Strateji:** `REDIS_AND_DATABASE`

```bash
# Disk'i kapat
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy/disk?enabled=false"
```

---

### Senaryo 5: Maintenance Mode

**Hedef:** Sadece memory cache, DB'ye yazma

**Strateji:** `MEMORY_ONLY`

```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"
```

---

## 🔄 Dinamik Strateji Değişimi

### Örnek: Gece Full Persistence, Gündüz Memory Only

```bash
# Sabah 08:00 - Memory only (hızlı)
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"

# Akşam 18:00 - Full persistence (güvenli)
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=FULL_PERSISTENCE"
```

### Cron Job ile Otomatik

```bash
# crontab -e
0 8 * * * curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"
0 18 * * * curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=FULL_PERSISTENCE"
```

---

## 📈 Monitoring

### Strateji Değişikliklerini İzle

```bash
# Her 5 saniyede bir kontrol et
watch -n 5 'curl -s http://localhost:8083/ProlizWebServices/api/cache-management/strategy | jq'
```

### Log'ları İzle

```bash
tail -f logs/proliz-web-services.log | grep "Cache strategy"
```

**Örnek Log:**
```
2025-10-07 09:46:23 - 📝 Cache strategy: MEMORY_ONLY (Redis only)
2025-10-07 10:15:45 - 📝 Cache strategy: FULL_PERSISTENCE (Redis + Disk + Database)
```

---

## ⚠️ Önemli Notlar

### 1. **Strateji Değişikliği Anında Geçerli**

Strateji değiştirdiğinizde:
- ✅ Yeni istekler hemen yeni stratejiyi kullanır
- ✅ Mevcut cache verileri korunur
- ✅ Uygulama restart gerektirmez

### 2. **Veri Kaybı Riski**

`MEMORY_ONLY` kullanırken:
- ⚠️ Restart'ta tüm cache kaybolur
- ⚠️ Redis çökerse veri kaybolur
- ✅ SOAP servisinden tekrar çekilir

### 3. **Performans Etkisi**

Strateji değiştirirken:
- `FULL_PERSISTENCE` → `MEMORY_ONLY`: Yazma hızlanır
- `MEMORY_ONLY` → `FULL_PERSISTENCE`: Yazma yavaşlar
- Okuma hızı her zaman optimize edilir

### 4. **Thread Safety**

- ✅ Tüm strateji değişiklikleri thread-safe
- ✅ `volatile` keyword kullanılır
- ✅ Concurrent requests güvenli

---

## 🎓 Best Practices

### 1. **Production'da FULL_PERSISTENCE Kullan**

```properties
# application.properties (production)
cache.strategy.redis.enabled=true
cache.strategy.disk.enabled=true
cache.strategy.database.enabled=true
```

### 2. **Development'ta MEMORY_ONLY Kullan**

```properties
# application-dev.properties
cache.strategy.redis.enabled=true
cache.strategy.disk.enabled=false
cache.strategy.database.enabled=false
```

### 3. **Monitoring Ekle**

```bash
# Health check
curl http://localhost:8083/ProlizWebServices/api/cache-management/health

# Strategy check
curl http://localhost:8083/ProlizWebServices/api/cache-management/strategy
```

### 4. **Fallback Planı Hazırla**

Redis çökerse otomatik olarak Disk + Database'e geç:

```bash
# Redis health check failed
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=DISK_AND_DATABASE"
```

---

## 🔍 Troubleshooting

### Redis Bağlanamıyor

**Çözüm:** Disk + Database'e geç
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=DISK_AND_DATABASE"
```

### Disk Doldu

**Çözüm:** Redis + Database'e geç
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=REDIS_AND_DATABASE"
```

### Database Yavaş

**Çözüm:** Memory only'e geç (geçici)
```bash
curl -X PUT "http://localhost:8083/ProlizWebServices/api/cache-management/strategy?strategy=MEMORY_ONLY"
```

---

## 📞 API Endpoint'leri Özeti

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/api/cache-management/strategy` | Mevcut stratejiyi getir |
| PUT | `/api/cache-management/strategy?strategy=X` | Strateji değiştir |
| PUT | `/api/cache-management/strategy/redis?enabled=X` | Redis aç/kapat |
| PUT | `/api/cache-management/strategy/disk?enabled=X` | Disk aç/kapat |
| PUT | `/api/cache-management/strategy/database?enabled=X` | Database aç/kapat |
| PUT | `/api/cache-management/strategy/custom?redis=X&disk=Y&database=Z` | Özel kombinasyon |

---

## 🎉 Sonuç

Artık cache stratejinizi **istediğiniz zaman** değiştirebilirsiniz:

- ✅ **Runtime'da** değiştirilebilir
- ✅ **Restart gerektirmez**
- ✅ **Thread-safe**
- ✅ **7 hazır strateji**
- ✅ **Özel kombinasyonlar**
- ✅ **REST API** ile yönetim
- ✅ **Swagger UI** desteği

**Kullanım:** Swagger UI'dan test edin!
http://localhost:8083/ProlizWebServices/swagger-ui.html
