package com.prolizwebservices.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prolizwebservices.entity.CachedSoapResponse;
import com.prolizwebservices.entity.CacheMetrics;
import com.prolizwebservices.repository.CachedSoapResponseRepository;
import com.prolizwebservices.repository.CacheMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Karma cache stratejisi implementasyonu
 * 
 * Cache Hierarchy (L1 -> L2 -> L3 -> Source):
 * 1. Redis (L1) - En hızlı, volatile
 * 2. Disk Cache (L2) - Orta hızlı, persistent
 * 3. Database (L3) - Yavaş, fully persistent
 * 4. SOAP Service (Source) - En yavaş, kaynak
 * 
 * Okuma Stratejisi:
 * - Önce Redis'ten kontrol et
 * - Redis'te yoksa Disk'ten kontrol et
 * - Disk'te yoksa Database'den kontrol et
 * - Hiçbirinde yoksa SOAP'tan çek
 * 
 * Yazma Stratejisi:
 * - Tüm katmanlara yaz (Redis + Disk + Database)
 * - Write-through pattern
 */
@Service
public class HybridCacheService {

    private static final Logger logger = LoggerFactory.getLogger(HybridCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CachedSoapResponseRepository cacheRepository;

    @Autowired
    private CacheMetricsRepository metricsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cache.strategy.redis.enabled:true}")
    private volatile boolean redisEnabled;

    @Value("${cache.strategy.disk.enabled:true}")
    private volatile boolean diskEnabled;

    @Value("${cache.strategy.database.enabled:true}")
    private volatile boolean databaseEnabled;

    @Value("${cache.disk.directory:./cache}")
    private String diskCacheDirectory;

    @Value("${cache.ttl.soap-response:86400}")
    private long defaultTtlSeconds;

    /**
     * Cache'ten veri oku veya kaynak'tan çek
     * 
     * @param cacheKey Cache anahtarı
     * @param serviceMethod Servis metodu adı
     * @param dataSupplier Veri kaynağı (SOAP çağrısı)
     * @return Cache'lenmiş veya yeni çekilmiş veri
     */
    public String getOrFetch(String cacheKey, String serviceMethod, Supplier<String> dataSupplier) {
        long startTime = System.currentTimeMillis();
        String result = null;
        String source = null;

        try {
            // L1: Redis Cache
            if (redisEnabled) {
                result = getFromRedis(cacheKey);
                if (result != null) {
                    source = "REDIS";
                    logger.debug("✅ Cache HIT (Redis): {}", cacheKey);
                    recordMetric(serviceMethod, "REDIS_HIT");
                    return result;
                }
            }

            // L2: Disk Cache
            if (diskEnabled) {
                result = getFromDisk(cacheKey);
                if (result != null) {
                    source = "DISK";
                    logger.debug("✅ Cache HIT (Disk): {}", cacheKey);
                    recordMetric(serviceMethod, "DISK_HIT");
                    
                    // Disk'ten bulundu, Redis'e de yaz (promotion)
                    if (redisEnabled) {
                        saveToRedis(cacheKey, result, defaultTtlSeconds);
                    }
                    return result;
                }
            }

            // L3: Database Cache
            if (databaseEnabled) {
                result = getFromDatabase(cacheKey);
                if (result != null) {
                    source = "DATABASE";
                    logger.debug("✅ Cache HIT (Database): {}", cacheKey);
                    recordMetric(serviceMethod, "DATABASE_HIT");
                    
                    // Database'den bulundu, üst katmanlara yaz (promotion)
                    if (diskEnabled) {
                        saveToDisk(cacheKey, result);
                    }
                    if (redisEnabled) {
                        saveToRedis(cacheKey, result, defaultTtlSeconds);
                    }
                    return result;
                }
            }

            // Cache MISS - SOAP'tan çek
            logger.info("❌ Cache MISS: {} - SOAP çağrısı yapılıyor", cacheKey);
            recordMetric(serviceMethod, "CACHE_MISS");
            recordMetric(serviceMethod, "SOAP_CALL");
            
            result = dataSupplier.get();
            source = "SOAP";

            // Tüm katmanlara yaz
            if (result != null && !result.isEmpty()) {
                saveToAllLayers(cacheKey, serviceMethod, result);
            }

            return result;

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Cache operation completed: {} ms (source: {})", duration, source);
        }
    }
    /**
     * Redis'ten veri oku
     */
    private String getFromRedis(String cacheKey) {
        try {
            Object value = redisTemplate.opsForValue().get(cacheKey);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.debug("Redis okuma hatası: {}", e.getMessage());
            // Redis bağlantı hatası varsa otomatik olarak devre dışı bırak
            if (e.getMessage() != null && e.getMessage().contains("Cannot get Jedis connection")) {
                if (redisEnabled) {
                    logger.warn("⚠️ Redis bağlantı hatası! Redis cache otomatik olarak devre dışı bırakıldı.");
                    redisEnabled = false;
                }
            }
            return null;
        }
    }

    /**
     * Redis'e veri yaz
     */
    private void saveToRedis(String cacheKey, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.debug("Redis yazma hatası: {}", e.getMessage());
            // Redis bağlantı hatası varsa otomatik olarak devre dışı bırak
            if (e.getMessage() != null && e.getMessage().contains("Cannot get Jedis connection")) {
                if (redisEnabled) {
                    logger.warn("⚠️ Redis bağlantı hatası! Redis cache otomatik olarak devre dışı bırakıldı.");
                    redisEnabled = false;
                }
            }
        }
    }

    /**
     * Disk'ten veri oku
     */
    private String getFromDisk(String cacheKey) {
        try {
            Path filePath = getDiskCachePath(cacheKey);
            if (Files.exists(filePath)) {
                return Files.readString(filePath);
            }
        } catch (IOException e) {
            logger.warn("Disk cache okuma hatası: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Database'den veri oku
     */
    private String getFromDatabase(String cacheKey) {
        try {
            Optional<CachedSoapResponse> cached = cacheRepository.findByCacheKey(cacheKey);
            if (cached.isPresent()) {
                CachedSoapResponse response = cached.get();
                
                // Süresi dolmuş mu kontrol et
                if (response.isExpired()) {
                    logger.debug("Database cache expired: {}", cacheKey);
                    return null;
                }
                
                // Erişim kaydı
                response.recordAccess();
                cacheRepository.save(response);
                
                return response.getXmlResponse();
            }
        } catch (Exception e) {
            logger.warn("Database cache okuma hatası: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Disk'e yaz
     */
    private void saveToDisk(String cacheKey, String data) {
        try {
            Path filePath = getDiskCachePath(cacheKey);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, data);
            logger.debug("✅ Disk'e yazıldı: {}", cacheKey);
        } catch (IOException e) {
            logger.error("Disk cache yazma hatası: {}", e.getMessage());
        }
    }

    /**
     * Database'e yaz
     */
    @Transactional
    private void saveToDatabase(String cacheKey, String serviceMethod, String data) {
        try {
            Optional<CachedSoapResponse> existing = cacheRepository.findByCacheKey(cacheKey);
            
            CachedSoapResponse response;
            if (existing.isPresent()) {
                // Güncelle
                response = existing.get();
                response.setXmlResponse(data);
                response.setLastAccessedAt(LocalDateTime.now());
                response.setExpiresAt(LocalDateTime.now().plusSeconds(defaultTtlSeconds));
                response.setStatus(CachedSoapResponse.CacheStatus.VALID);
            } else {
                // Yeni oluştur
                response = new CachedSoapResponse();
                response.setCacheKey(cacheKey);
                response.setServiceMethod(serviceMethod);
                response.setXmlResponse(data);
                response.setCreatedAt(LocalDateTime.now());
                response.setLastAccessedAt(LocalDateTime.now());
                response.setExpiresAt(LocalDateTime.now().plusSeconds(defaultTtlSeconds));
                response.setResponseSize((long) data.length());
                response.setStatus(CachedSoapResponse.CacheStatus.VALID);
                response.setAccessCount(0L);
            }
            
            cacheRepository.save(response);
            logger.debug("✅ Database'e yazıldı: {}", cacheKey);
            
        } catch (Exception e) {
            logger.error("Database cache yazma hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Tüm katmanlara yaz (Write-through)
     */
    private void saveToAllLayers(String cacheKey, String serviceMethod, String data) {
        if (redisEnabled) {
            saveToRedis(cacheKey, data, defaultTtlSeconds);
        }
        if (diskEnabled) {
            saveToDisk(cacheKey, data);
        }
        if (databaseEnabled) {
            saveToDatabase(cacheKey, serviceMethod, data);
        }
        logger.info("✅ Cache yazıldı (tüm katmanlar): {}", cacheKey);
    }

    /**
     * Cache'i invalidate et (tüm katmanlardan sil)
     */
    public void invalidate(String cacheKey) {
        logger.info("🗑️ Cache invalidate: {}", cacheKey);
        
        if (redisEnabled) {
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                logger.warn("Redis delete hatası: {}", e.getMessage());
            }
        }
        
        if (diskEnabled) {
            try {
                Path filePath = getDiskCachePath(cacheKey);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                logger.warn("Disk cache delete hatası: {}", e.getMessage());
            }
        }
        
        if (databaseEnabled) {
            try {
                cacheRepository.findByCacheKey(cacheKey).ifPresent(cacheRepository::delete);
            } catch (Exception e) {
                logger.warn("Database cache delete hatası: {}", e.getMessage());
            }
        }
    }

    /**
     * Servis metoduna göre tüm cache'leri invalidate et
     */
    @Transactional
    public void invalidateByServiceMethod(String serviceMethod) {
        logger.info("🗑️ Cache invalidate (service): {}", serviceMethod);
        
        if (databaseEnabled) {
            try {
                var caches = cacheRepository.findByServiceMethod(serviceMethod);
                for (var cache : caches) {
                    invalidate(cache.getCacheKey());
                }
            } catch (Exception e) {
                logger.error("Service method cache invalidate hatası: {}", e.getMessage());
            }
        }
    }

    /**
     * Disk cache dosya yolu
     */
    private Path getDiskCachePath(String cacheKey) {
        String safeKey = cacheKey.replaceAll("[^a-zA-Z0-9_-]", "_");
        return Paths.get(diskCacheDirectory, safeKey + ".cache");
    }

    /**
     * Metrik kaydet
     */
    private void recordMetric(String serviceMethod, String metricType) {
        try {
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            Optional<CacheMetrics> existing = metricsRepository.findByMetricDateAndServiceMethod(today, serviceMethod);
            CacheMetrics metrics = existing.orElse(new CacheMetrics());
            
            if (!existing.isPresent()) {
                metrics.setMetricDate(today);
                metrics.setServiceMethod(serviceMethod);
            }
            
            switch (metricType) {
                case "REDIS_HIT":
                    metrics.setRedisHits(metrics.getRedisHits() + 1);
                    metrics.setCacheHits(metrics.getCacheHits() + 1);
                    break;
                case "DISK_HIT":
                    metrics.setDiskHits(metrics.getDiskHits() + 1);
                    metrics.setCacheHits(metrics.getCacheHits() + 1);
                    break;
                case "DATABASE_HIT":
                    metrics.setDatabaseHits(metrics.getDatabaseHits() + 1);
                    metrics.setCacheHits(metrics.getCacheHits() + 1);
                    break;
                case "CACHE_MISS":
                    metrics.setCacheMisses(metrics.getCacheMisses() + 1);
                    break;
                case "SOAP_CALL":
                    metrics.setSoapCalls(metrics.getSoapCalls() + 1);
                    break;
            }
            
            metricsRepository.save(metrics);
            
        } catch (Exception e) {
            logger.debug("Metrik kaydetme hatası (non-critical): {}", e.getMessage());
        }
    }

    /**
     * Cache istatistiklerini getir
     */
    public CacheStatistics getStatistics() {
        CacheStatistics stats = new CacheStatistics();
        
        try {
            if (databaseEnabled) {
                stats.setTotalCachedItems(cacheRepository.count());
                stats.setTotalCacheSize(cacheRepository.getTotalCacheSize());
                stats.setValidCaches(cacheRepository.countByStatus(CachedSoapResponse.CacheStatus.VALID));
                stats.setExpiredCaches(cacheRepository.countByStatus(CachedSoapResponse.CacheStatus.EXPIRED));
            }
            
            // Son 24 saatin metrikleri
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            var recentMetrics = metricsRepository.findMetricsBetween(yesterday, LocalDateTime.now());
            
            long totalHits = recentMetrics.stream().mapToLong(CacheMetrics::getCacheHits).sum();
            long totalMisses = recentMetrics.stream().mapToLong(CacheMetrics::getCacheMisses).sum();
            
            stats.setTotalHits(totalHits);
            stats.setTotalMisses(totalMisses);
            stats.setHitRate(totalHits + totalMisses == 0 ? 0.0 : (totalHits * 100.0) / (totalHits + totalMisses));
            
        } catch (Exception e) {
            logger.error("İstatistik hesaplama hatası: {}", e.getMessage());
        }
        
        return stats;
    }

    /**
     * Cache istatistikleri için inner class
     */
    public static class CacheStatistics {
        private long totalCachedItems;
        private Long totalCacheSize;
        private long validCaches;
        private long expiredCaches;
        private long totalHits;
        private long totalMisses;
        private double hitRate;

        // Getters and Setters
        public long getTotalCachedItems() { return totalCachedItems; }
        public void setTotalCachedItems(long totalCachedItems) { this.totalCachedItems = totalCachedItems; }
        
        public Long getTotalCacheSize() { return totalCacheSize; }
        public void setTotalCacheSize(Long totalCacheSize) { this.totalCacheSize = totalCacheSize; }
        
        public long getValidCaches() { return validCaches; }
        public void setValidCaches(long validCaches) { this.validCaches = validCaches; }
        
        public long getExpiredCaches() { return expiredCaches; }
        public void setExpiredCaches(long expiredCaches) { this.expiredCaches = expiredCaches; }
        
        public long getTotalHits() { return totalHits; }
        public void setTotalHits(long totalHits) { this.totalHits = totalHits; }
        
        public long getTotalMisses() { return totalMisses; }
        public void setTotalMisses(long totalMisses) { this.totalMisses = totalMisses; }
        
        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }
    }
    
    // ============================================
    // RUNTIME CACHE STRATEGY MANAGEMENT
    // ============================================
    
    /**
     * Redis cache'i runtime'da aç/kapat
     */
    public void setRedisEnabled(boolean enabled) {
        this.redisEnabled = enabled;
        logger.info("🔧 Redis cache {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Disk cache'i runtime'da aç/kapat
     */
    public void setDiskEnabled(boolean enabled) {
        this.diskEnabled = enabled;
        logger.info("🔧 Disk cache {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Database cache'i runtime'da aç/kapat
     */
    public void setDatabaseEnabled(boolean enabled) {
        this.databaseEnabled = enabled;
        logger.info("🔧 Database cache {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Tüm cache katmanlarını aç/kapat
     */
    public void setAllCacheLayers(boolean redis, boolean disk, boolean database) {
        this.redisEnabled = redis;
        this.diskEnabled = disk;
        this.databaseEnabled = database;
        logger.info("🔧 Cache layers updated: Redis={}, Disk={}, Database={}", redis, disk, database);
    }
    
    /**
     * Cache stratejisini değiştir
     * 
     * @param strategy "MEMORY_ONLY", "FULL_PERSISTENCE", "REDIS_ONLY", "DISK_ONLY", "DATABASE_ONLY"
     */
    public void setCacheStrategy(String strategy) {
        switch (strategy.toUpperCase()) {
            case "MEMORY_ONLY":
                // Sadece Redis (en hızlı, restart'ta kaybolur)
                setAllCacheLayers(true, false, false);
                logger.info("📝 Cache strategy: MEMORY_ONLY (Redis only)");
                break;
                
            case "FULL_PERSISTENCE":
                // Tüm katmanlar (en güvenli, restart-safe)
                setAllCacheLayers(true, true, true);
                logger.info("📝 Cache strategy: FULL_PERSISTENCE (Redis + Disk + Database)");
                break;
                
            case "REDIS_ONLY":
                // Sadece Redis
                setAllCacheLayers(true, false, false);
                logger.info("📝 Cache strategy: REDIS_ONLY");
                break;
                
            case "DISK_ONLY":
                // Sadece Disk
                setAllCacheLayers(false, true, false);
                logger.info("📝 Cache strategy: DISK_ONLY");
                break;
                
            case "DATABASE_ONLY":
                // Sadece Database
                setAllCacheLayers(false, false, true);
                logger.info("📝 Cache strategy: DATABASE_ONLY");
                break;
                
            case "DISK_AND_DATABASE":
                // Disk + Database (Redis yok)
                setAllCacheLayers(false, true, true);
                logger.info("📝 Cache strategy: DISK_AND_DATABASE");
                break;
                
            case "REDIS_AND_DATABASE":
                // Redis + Database (Disk yok)
                setAllCacheLayers(true, false, true);
                logger.info("📝 Cache strategy: REDIS_AND_DATABASE");
                break;
                
            default:
                logger.warn("⚠️ Unknown cache strategy: {}. Using FULL_PERSISTENCE", strategy);
                setAllCacheLayers(true, true, true);
        }
    }
    
    /**
     * Mevcut cache stratejisini al
     */
    public CacheStrategyInfo getCacheStrategy() {
        CacheStrategyInfo info = new CacheStrategyInfo();
        info.setRedisEnabled(redisEnabled);
        info.setDiskEnabled(diskEnabled);
        info.setDatabaseEnabled(databaseEnabled);
        
        // Strateji adını belirle
        if (redisEnabled && diskEnabled && databaseEnabled) {
            info.setStrategyName("FULL_PERSISTENCE");
        } else if (redisEnabled && !diskEnabled && !databaseEnabled) {
            info.setStrategyName("MEMORY_ONLY");
        } else if (!redisEnabled && diskEnabled && databaseEnabled) {
            info.setStrategyName("DISK_AND_DATABASE");
        } else if (redisEnabled && !diskEnabled && databaseEnabled) {
            info.setStrategyName("REDIS_AND_DATABASE");
        } else if (!redisEnabled && diskEnabled && !databaseEnabled) {
            info.setStrategyName("DISK_ONLY");
        } else if (!redisEnabled && !diskEnabled && databaseEnabled) {
            info.setStrategyName("DATABASE_ONLY");
        } else {
            info.setStrategyName("CUSTOM");
        }
        
        return info;
    }
    
    /**
     * Cache stratejisi bilgisi için inner class
     */
    public static class CacheStrategyInfo {
        private String strategyName;
        private boolean redisEnabled;
        private boolean diskEnabled;
        private boolean databaseEnabled;
        
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        
        public boolean isRedisEnabled() { return redisEnabled; }
        public void setRedisEnabled(boolean redisEnabled) { this.redisEnabled = redisEnabled; }
        
        public boolean isDiskEnabled() { return diskEnabled; }
        public void setDiskEnabled(boolean diskEnabled) { this.diskEnabled = diskEnabled; }
        
        public boolean isDatabaseEnabled() { return databaseEnabled; }
        public void setDatabaseEnabled(boolean databaseEnabled) { this.databaseEnabled = databaseEnabled; }
    }
}
