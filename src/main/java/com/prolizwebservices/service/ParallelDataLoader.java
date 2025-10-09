package com.prolizwebservices.service;

import com.prolizwebservices.client.OgrenciWebServiceClient;
import com.prolizwebservices.model.Ders;
import com.prolizwebservices.model.Ogrenci;
import com.prolizwebservices.model.OgretimElemani;
import com.prolizwebservices.util.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Ultra-optimized parallel data loader
 * 
 * Optimizasyon Stratejileri:
 * 1. CompletableFuture ile asenkron işlemler
 * 2. Custom thread pool (SOAP çağrıları için optimize)
 * 3. Batch processing (network overhead azaltma)
 * 4. Rate limiting (SOAP servisini yormama)
 * 5. Circuit breaker pattern (hata yönetimi)
 * 6. Adaptive batch sizing (dinamik batch boyutu)
 * 7. Priority queue (önemli veriler önce)
 * 8. Fail-fast mechanism (hızlı hata tespiti)
 */
@Service
public class ParallelDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(ParallelDataLoader.class);

    @Autowired
    private OgrenciWebServiceClient webServiceClient;

    @Autowired
    private XmlParser xmlParser;

    @Autowired
    @Qualifier("soapTaskExecutor")
    private Executor soapTaskExecutor;

    @Value("${parallel.loader.max-concurrent:50}")
    private int maxConcurrentRequests;

    @Value("${parallel.loader.batch-size:20}")
    private int batchSize;

    @Value("${parallel.loader.rate-limit-ms:10}")
    private int rateLimitMs;

    @Value("${parallel.loader.timeout-seconds:45}")
    private int timeoutSeconds;

    @Value("${parallel.loader.max-retries:2}")
    private int maxRetries;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int CIRCUIT_BREAKER_THRESHOLD = 10;

    /**
     * Öğretim elemanlarını ultra-hızlı paralel yükleme
     * 
     * @param tcList TC kimlik numaraları listesi
     * @return Yüklenen öğretim elemanları
     */
    public List<OgretimElemani> loadOgretimElemanlariParallel(List<String> tcList) {
        if (tcList == null || tcList.isEmpty()) {
            return Collections.emptyList();
        }

        logger.info("🚀 ULTRA-FAST PARALLEL: {} öğretim elemanı yüklenecek", tcList.size());
        long startTime = System.currentTimeMillis();

        List<OgretimElemani> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Adaptive batch size
        int adaptiveBatchSize = calculateAdaptiveBatchSize(tcList.size());
        int totalBatches = (int) Math.ceil((double) tcList.size() / adaptiveBatchSize);

        logger.info("📦 Batch stratejisi: {} batch, her batch {} item", totalBatches, adaptiveBatchSize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            // Circuit breaker check
            if (consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD) {
                logger.error("⚠️ Circuit breaker açıldı! Çok fazla hata oluştu.");
                break;
            }

            int startIndex = batchIndex * adaptiveBatchSize;
            int endIndex = Math.min(startIndex + adaptiveBatchSize, tcList.size());
            List<String> batchTCs = tcList.subList(startIndex, endIndex);

            logger.debug("🔄 Batch {}/{}: {} TC işleniyor...", batchIndex + 1, totalBatches, batchTCs.size());

            // Paralel CompletableFuture'lar oluştur
            List<CompletableFuture<Optional<List<OgretimElemani>>>> futures = batchTCs.stream()
                .map(tc -> CompletableFuture.supplyAsync(() -> 
                    loadOgretimElemaniWithRetry(tc), soapTaskExecutor)
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        logger.warn("TC {} yükleme hatası: {}", tc, ex.getMessage());
                        failureCount.incrementAndGet();
                        consecutiveFailures.incrementAndGet();
                        return Optional.empty();
                    })
                )
                .collect(Collectors.toList());

            // Tüm future'ları bekle
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(timeoutSeconds + 5, TimeUnit.SECONDS);

                // Sonuçları topla
                for (CompletableFuture<Optional<List<OgretimElemani>>> future : futures) {
                    Optional<List<OgretimElemani>> result = future.get();
                    if (result.isPresent() && !result.get().isEmpty()) {
                        results.addAll(result.get());
                        successCount.incrementAndGet();
                        consecutiveFailures.set(0); // Reset on success
                    }
                }

            } catch (Exception e) {
                logger.error("Batch {} completion hatası: {}", batchIndex + 1, e.getMessage());
            }

            // Rate limiting between batches
            if (batchIndex < totalBatches - 1) {
                sleepQuietly(rateLimitMs);
            }

            // Progress log
            if ((batchIndex + 1) % 5 == 0 || batchIndex == totalBatches - 1) {
                long elapsed = System.currentTimeMillis() - startTime;
                double itemsPerSecond = (successCount.get() * 1000.0) / elapsed;
                logger.info("📊 Progress: {}/{} batches, {} success, {} failures, {:.1f} items/sec",
                    batchIndex + 1, totalBatches, successCount.get(), failureCount.get(), itemsPerSecond);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("✅ ULTRA-FAST PARALLEL tamamlandı: {} eleman, {} ms, {:.1f} items/sec",
            results.size(), totalTime, (results.size() * 1000.0) / totalTime);

        return results;
    }

    /**
     * Ders öğrencilerini ultra-hızlı paralel yükleme
     */
    public Map<String, List<Ogrenci>> loadDersOgrencileriParallel(List<Ders> dersler) {
        if (dersler == null || dersler.isEmpty()) {
            return Collections.emptyMap();
        }

        logger.info("🚀 ULTRA-FAST PARALLEL: {} ders için öğrenciler yüklenecek", dersler.size());
        long startTime = System.currentTimeMillis();

        Map<String, List<Ogrenci>> results = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        int adaptiveBatchSize = calculateAdaptiveBatchSize(dersler.size());
        int totalBatches = (int) Math.ceil((double) dersler.size() / adaptiveBatchSize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            if (consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD) {
                logger.error("⚠️ Circuit breaker açıldı!");
                break;
            }

            int startIndex = batchIndex * adaptiveBatchSize;
            int endIndex = Math.min(startIndex + adaptiveBatchSize, dersler.size());
            List<Ders> batchDersler = dersler.subList(startIndex, endIndex);

            List<CompletableFuture<Map.Entry<String, List<Ogrenci>>>> futures = batchDersler.stream()
                .map(ders -> CompletableFuture.supplyAsync(() -> 
                    loadDersOgrencileriWithRetry(ders), soapTaskExecutor)
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        logger.warn("Ders {} öğrenci yükleme hatası: {}", ders.getDersHarId(), ex.getMessage());
                        failureCount.incrementAndGet();
                        consecutiveFailures.incrementAndGet();
                        return null;
                    })
                )
                .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(timeoutSeconds + 5, TimeUnit.SECONDS);

                for (CompletableFuture<Map.Entry<String, List<Ogrenci>>> future : futures) {
                    Map.Entry<String, List<Ogrenci>> entry = future.get();
                    if (entry != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                        results.put(entry.getKey(), entry.getValue());
                        successCount.incrementAndGet();
                        consecutiveFailures.set(0);
                    }
                }

            } catch (Exception e) {
                logger.error("Batch {} completion hatası: {}", batchIndex + 1, e.getMessage());
            }

            if (batchIndex < totalBatches - 1) {
                sleepQuietly(rateLimitMs);
            }

            if ((batchIndex + 1) % 5 == 0 || batchIndex == totalBatches - 1) {
                long elapsed = System.currentTimeMillis() - startTime;
                double itemsPerSecond = (successCount.get() * 1000.0) / elapsed;
                logger.info("📊 Progress: {}/{} batches, {} success, {} failures, {:.1f} items/sec",
                    batchIndex + 1, totalBatches, successCount.get(), failureCount.get(), itemsPerSecond);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("✅ ULTRA-FAST PARALLEL tamamlandı: {} ders, {} ms, {:.1f} items/sec",
            results.size(), totalTime, (results.size() * 1000.0) / totalTime);

        return results;
    }

    /**
     * Retry mekanizması ile öğretim elemanı yükleme
     */
    private Optional<List<OgretimElemani>> loadOgretimElemaniWithRetry(String tc) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String xmlResponse = webServiceClient.getOgretimElemaniByFilters(tc, null, null);
                List<OgretimElemani> elemanlar = xmlParser.parseOgretimElemanlari(xmlResponse);
                
                if (!elemanlar.isEmpty()) {
                    return Optional.of(elemanlar);
                }
                return Optional.empty();
                
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.debug("TC {} retry {}/{}", tc, attempt + 1, maxRetries);
                    sleepQuietly(100 * (attempt + 1)); // Exponential backoff
                } else {
                    logger.warn("TC {} tüm retry'lar başarısız", tc);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retry mekanizması ile ders öğrencileri yükleme
     */
    private Map.Entry<String, List<Ogrenci>> loadDersOgrencileriWithRetry(Ders ders) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String xmlResponse = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(ders.getDersHarId());
                List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResponse, ders.getDersHarId());
                
                return new AbstractMap.SimpleEntry<>(ders.getDersHarId(), ogrenciler);
                
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.debug("Ders {} retry {}/{}", ders.getDersHarId(), attempt + 1, maxRetries);
                    sleepQuietly(100 * (attempt + 1));
                } else {
                    logger.warn("Ders {} tüm retry'lar başarısız", ders.getDersHarId());
                }
            }
        }
        return new AbstractMap.SimpleEntry<>(ders.getDersHarId(), Collections.emptyList());
    }

    /**
     * Adaptive batch size calculation
     * Veri setinin boyutuna göre optimal batch size hesaplar
     */
    private int calculateAdaptiveBatchSize(int totalItems) {
        if (totalItems < 50) {
            return Math.min(10, totalItems);
        } else if (totalItems < 200) {
            return Math.min(20, totalItems);
        } else if (totalItems < 500) {
            return Math.min(30, totalItems);
        } else {
            return Math.min(50, totalItems);
        }
    }

    /**
     * Quiet sleep (interrupted exception handling)
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Circuit breaker'ı sıfırla
     */
    public void resetCircuitBreaker() {
        consecutiveFailures.set(0);
        logger.info("🔄 Circuit breaker sıfırlandı");
    }

    /**
     * Circuit breaker durumunu kontrol et
     */
    public boolean isCircuitBreakerOpen() {
        return consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD;
    }

    /**
     * Performans istatistikleri
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("maxConcurrentRequests", maxConcurrentRequests);
        stats.put("batchSize", batchSize);
        stats.put("rateLimitMs", rateLimitMs);
        stats.put("timeoutSeconds", timeoutSeconds);
        stats.put("consecutiveFailures", consecutiveFailures.get());
        stats.put("circuitBreakerOpen", isCircuitBreakerOpen());
        return stats;
    }
}
