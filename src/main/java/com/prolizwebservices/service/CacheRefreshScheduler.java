package com.prolizwebservices.service;

import com.prolizwebservices.entity.CachedSoapResponse;
import com.prolizwebservices.repository.CachedSoapResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cache'lerin otomatik olarak yenilenmesini sağlayan scheduler servis
 * 
 * Görevler:
 * 1. Süresi dolmuş cache'leri tespit et
 * 2. Popüler cache'leri arka planda yenile
 * 3. Eski cache'leri temizle
 * 4. Cache sağlığını kontrol et
 */
@Service
public class CacheRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CacheRefreshScheduler.class);

    @Autowired
    private CachedSoapResponseRepository cacheRepository;

    @Autowired
    private HybridCacheService cacheService;

    @Autowired(required = false)
    private DataCacheService dataCacheService;

    @Value("${cache.refresh.auto-enabled:true}")
    private boolean autoRefreshEnabled;

    /**
     * Süresi dolmuş cache'leri tespit et ve işaretle
     * Her 10 dakikada bir çalışır
     */
    @Scheduled(fixedRate = 600000) // 10 dakika
    @Transactional
    public void markExpiredCaches() {
        if (!autoRefreshEnabled) {
            return;
        }

        try {
            logger.debug("🔍 Süresi dolmuş cache'ler kontrol ediliyor...");
            
            List<CachedSoapResponse> expiredCaches = cacheRepository.findExpiredCaches(LocalDateTime.now());
            
            if (!expiredCaches.isEmpty()) {
                for (CachedSoapResponse cache : expiredCaches) {
                    cache.setStatus(CachedSoapResponse.CacheStatus.EXPIRED);
                }
                cacheRepository.saveAll(expiredCaches);
                
                logger.info("⏰ {} cache süresi dolmuş olarak işaretlendi", expiredCaches.size());
            }
            
        } catch (Exception e) {
            logger.error("Expired cache marking hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Popüler cache'leri arka planda yenile
     * Her saat başı çalışır
     */
    @Scheduled(cron = "0 0 * * * ?") // Her saat başı
    @Transactional
    public void refreshPopularCaches() {
        if (!autoRefreshEnabled) {
            return;
        }

        try {
            logger.info("🔄 Popüler cache'ler yenileniyor...");
            
            // En çok erişilen cache'leri bul
            List<CachedSoapResponse> popularCaches = cacheRepository.findAll().stream()
                .filter(c -> c.getAccessCount() > 10) // 10'dan fazla erişim
                .filter(CachedSoapResponse::isExpired)
                .limit(50) // En fazla 50 cache
                .toList();
            
            int refreshed = 0;
            for (CachedSoapResponse cache : popularCaches) {
                try {
                    // Cache'i yenile (bu SOAP çağrısı tetikleyecek)
                    cache.setStatus(CachedSoapResponse.CacheStatus.REFRESHING);
                    cacheRepository.save(cache);
                    
                    // Invalidate et - bir sonraki erişimde otomatik yenilenecek
                    cacheService.invalidate(cache.getCacheKey());
                    refreshed++;
                    
                    // Rate limiting
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    logger.warn("Cache refresh hatası ({}): {}", cache.getCacheKey(), e.getMessage());
                }
            }
            
            logger.info("✅ {} popüler cache yenilendi", refreshed);
            
        } catch (Exception e) {
            logger.error("Popular cache refresh hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Eski cache'leri temizle
     * Her gün gece 2'de çalışır
     */
    @Scheduled(cron = "${cache.refresh.cron:0 0 2 * * ?}") // Gece 2:00
    @Transactional
    public void cleanupOldCaches() {
        if (!autoRefreshEnabled) {
            return;
        }

        try {
            logger.info("🧹 Eski cache'ler temizleniyor...");
            
            // 30 günden eski cache'leri sil
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            cacheRepository.deleteOldCaches(thirtyDaysAgo);
            
            logger.info("✅ 30 günden eski cache'ler temizlendi");
            
        } catch (Exception e) {
            logger.error("Cache cleanup hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * LRU (Least Recently Used) cache temizliği
     * Cache boyutu çok büyükse en az kullanılanları sil
     * Her 6 saatte bir çalışır
     */
    @Scheduled(fixedRate = 21600000) // 6 saat
    @Transactional
    public void lruCleanup() {
        if (!autoRefreshEnabled) {
            return;
        }

        try {
            Long totalSize = cacheRepository.getTotalCacheSize();
            long maxSizeBytes = 500L * 1024 * 1024; // 500 MB
            
            if (totalSize != null && totalSize > maxSizeBytes) {
                logger.info("🗑️ Cache boyutu limiti aşıldı ({} MB), LRU temizliği başlatılıyor...", 
                    totalSize / (1024 * 1024));
                
                // En az kullanılan cache'leri bul
                List<CachedSoapResponse> lruCaches = cacheRepository.findLeastRecentlyUsed();
                
                long deletedSize = 0;
                int deletedCount = 0;
                
                for (CachedSoapResponse cache : lruCaches) {
                    if (totalSize - deletedSize <= maxSizeBytes * 0.8) {
                        break; // %80'e düştü, yeter
                    }
                    
                    deletedSize += cache.getResponseSize();
                    cacheService.invalidate(cache.getCacheKey());
                    cacheRepository.delete(cache);
                    deletedCount++;
                }
                
                logger.info("✅ LRU temizliği tamamlandı: {} cache silindi ({} MB)", 
                    deletedCount, deletedSize / (1024 * 1024));
            }
            
        } catch (Exception e) {
            logger.error("LRU cleanup hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Cache sağlık kontrolü
     * Her 30 dakikada bir çalışır
     */
    @Scheduled(fixedRate = 1800000) // 30 dakika
    public void healthCheck() {
        if (!autoRefreshEnabled) {
            return;
        }

        try {
            HybridCacheService.CacheStatistics stats = cacheService.getStatistics();
            
            logger.info("📊 Cache Health: Hit Rate: {:.2f}%, Total Items: {}, Valid: {}, Expired: {}",
                String.format("%.2f", stats.getHitRate()), 
                stats.getTotalCachedItems(),
                stats.getValidCaches(),
                stats.getExpiredCaches());
            
            // Düşük hit rate uyarısı
            if (stats.getHitRate() < 30.0) {
                logger.warn("⚠️ Cache hit rate düşük: {:.2f}% - Cache stratejisi gözden geçirilmeli", 
                    String.format("%.2f", stats.getHitRate()));
            }
            
            // Çok fazla expired cache uyarısı
            if (stats.getExpiredCaches() > stats.getValidCaches()) {
                logger.warn("⚠️ Expired cache sayısı valid cache'ten fazla - Temizlik gerekli");
            }
            
        } catch (Exception e) {
            logger.error("Health check hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * DataCache'i periyodik olarak yenile
     * Her gün gece 3'te çalışır
     */
    @Scheduled(cron = "0 0 3 * * ?") // Gece 3:00
    public void refreshDataCache() {
        if (!autoRefreshEnabled || dataCacheService == null) {
            return;
        }

        try {
            logger.info("🔄 DataCache yenileniyor...");
            dataCacheService.refreshCache();
            logger.info("✅ DataCache yenileme tamamlandı");
            
        } catch (Exception e) {
            logger.error("DataCache refresh hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Manuel cache refresh tetikle
     */
    public void triggerManualRefresh() {
        logger.info("🔄 Manuel cache refresh başlatıldı");
        
        try {
            // Tüm expired cache'leri invalidate et
            List<CachedSoapResponse> expiredCaches = cacheRepository.findExpiredCaches(LocalDateTime.now());
            for (CachedSoapResponse cache : expiredCaches) {
                cacheService.invalidate(cache.getCacheKey());
            }
            
            // DataCache'i yenile
            if (dataCacheService != null) {
                dataCacheService.refreshCache();
            }
            
            logger.info("✅ Manuel cache refresh tamamlandı");
            
        } catch (Exception e) {
            logger.error("Manuel refresh hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Cache refresh failed: " + e.getMessage(), e);
        }
    }
}
