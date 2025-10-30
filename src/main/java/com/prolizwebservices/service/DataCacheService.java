package com.prolizwebservices.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.prolizwebservices.client.OgrenciWebServiceClient;
import com.prolizwebservices.model.Ders;
import com.prolizwebservices.model.Ogrenci;
import com.prolizwebservices.model.OgretimElemani;
import com.prolizwebservices.util.XmlParser;

import jakarta.annotation.PostConstruct;

/**
 * SOAP verilerini cache'leyen ve organize eden servis
 */
@Service
public class DataCacheService {

    private static final Logger logger = LoggerFactory.getLogger(DataCacheService.class);

    @Autowired
    private OgrenciWebServiceClient webServiceClient;

    @Autowired
    private XmlParser xmlParser;
    
    @Autowired
    @Qualifier("soapTaskExecutor")
    private Executor soapTaskExecutor;
    
    @Autowired(required = false)
    private ParallelDataLoader parallelDataLoader;
    
    // 🚀 NEW: Progressive Loading Configuration
    @Value("${cache.preload.initial-courses:100}")
    private int initialCoursesToLoad;
    
    @Value("${cache.progressive.enabled:true}")
    private boolean progressiveLoadingEnabled;
    
    @Value("${cache.progressive.batch-size:50}")
    private int batchSize;
    
    @Value("${cache.progressive.rate-limit-ms:30}")
    private int rateLimitMs;
    
    @Value("${cache.progressive.max-errors:20}")
    private int maxErrorsPerBatch;
    
    // 📊 Progress tracking
    private volatile int totalCoursesProcessed = 0;
    private volatile int nextBatchStartIndex = 0;
    private volatile boolean backgroundLoadingComplete = false;

    // Cache data structures
    private final List<Ders> allDersler = Collections.synchronizedList(new ArrayList<>());
    private final List<OgretimElemani> allOgretimElemanlari = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<Ogrenci>> dersOgrencileriMap = new ConcurrentHashMap<>();

    // Index'ler - hızlı arama için
    private final Map<String, Ders> dersHarIdIndex = new ConcurrentHashMap<>();
    private final Map<String, List<Ders>> fakulteDerslerIndex = new ConcurrentHashMap<>();
    private final Map<String, List<Ders>> programDerslerIndex = new ConcurrentHashMap<>();
    private final Map<String, OgretimElemani> tcKimlikIndex = new ConcurrentHashMap<>();
    private final Map<String, OgretimElemani> sicilNoIndex = new ConcurrentHashMap<>();
    private final Map<String, List<OgretimElemani>> fakulteOgretimElemaniIndex = new ConcurrentHashMap<>();
    
    // 🚀 PERFORMANCE: Öğrenci bazlı ders index'i (çok hızlı arama için)
    private final Map<String, List<Ders>> ogrenciDerslerIndex = new ConcurrentHashMap<>();

    private LocalDateTime lastUpdateTime;
    private volatile boolean isInitialized = false;

    /**
     * Uygulama başlarken cache'i initialize et
     * 
     * DOĞRU SIRALAMA (Bağımlılık Zinciri):
     * 1. UzaktanEgitimDersleri (DERS_HAR_ID üretir)
     * 2. DersiVerenOgretimElamaniGetir (OGRETIM_ELEMANI_TC kullanır)
     * 3. UzaktanEgitimDersiAlanOgrencileri (DERS_HAR_ID kullanır)
     */
    @PostConstruct
    public void initializeCache() {
        logger.info("🚀 DataCache initialize ediliyor (Bağımlılık Zinciri Sırasıyla)...");
        
        try {
            // ============================================
            // STEP 1: UzaktanEgitimDersleri
            // ============================================
            logger.info("📋 STEP 1/4: Dersler yükleniyor (UzaktanEgitimDersleri)...");
            loadDersler();
            logger.info("✅ STEP 1/4: {} ders yüklendi", allDersler.size());
            
            // ============================================
            // STEP 2: DersiVerenOgretimElamaniGetir
            // ============================================
            logger.info("👨‍🏫 STEP 2/4: Öğretim elemanları yükleniyor (DersiVerenOgretimElamaniGetir)...");
            logger.info("   → Derslerden {} benzersiz TC kimlik numarası çıkarılacak", 
                allDersler.stream()
                    .map(Ders::getOgretimElemaniTC)
                    .filter(tc -> tc != null && !tc.trim().isEmpty())
                    .distinct()
                    .count());
            loadOgretimElemanlari();
            logger.info("✅ STEP 2/4: {} öğretim elemanı yüklendi", allOgretimElemanlari.size());
            
            // ============================================
            // STEP 3: Index'leri Oluştur
            // ============================================
            logger.info("🔍 STEP 3/4: Index'ler oluşturuluyor...");
            buildIndexes();
            logger.info("✅ STEP 3/4: Index'ler oluşturuldu");
            
            // ============================================
            // STEP 4: UzaktanEgitimDersiAlanOgrencileri
            // ============================================
            logger.info("👥 STEP 4/4: Ders öğrencileri yükleniyor (UzaktanEgitimDersiAlanOgrencileri)...");
            logger.info("   → İlk {} ders için öğrenciler yüklenecek (DERS_HAR_ID kullanılarak)", initialCoursesToLoad);
            loadSelectedDersOgrencileri();
            logger.info("✅ STEP 4/4: {} ders için öğrenciler yüklendi", dersOgrencileriMap.size());
            
            // ============================================
            // Tamamlandı
            // ============================================
            lastUpdateTime = LocalDateTime.now();
            isInitialized = true;
            
            logger.info("🎉 DataCache initialize tamamlandı!");
            logger.info("   📊 Özet:");
            logger.info("      - Dersler: {}", allDersler.size());
            logger.info("      - Öğretim Elemanları: {}", allOgretimElemanlari.size());
            logger.info("      - Ders-Öğrenci İlişkileri: {}", dersOgrencileriMap.size());
            logger.info("      - Toplam Öğrenci: {}", 
                dersOgrencileriMap.values().stream().mapToInt(List::size).sum());
                
        } catch (Exception e) {
            logger.error("❌ DataCache initialize hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Dersleri SOAP'tan çeker ve parse eder
     */
    private void loadDersler() {
        try {
            logger.info("Dersler yükleniyor...");
            String xmlResponse = webServiceClient.getUzaktanEgitimDersleri();
            List<Ders> dersler = xmlParser.parseDersler(xmlResponse);
            
            allDersler.clear();
            allDersler.addAll(dersler);
            
            logger.info("{} ders yüklendi", dersler.size());
            
        } catch (Exception e) {
            logger.error("Ders yükleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * 🚀 ULTRA-FAST PARALEL: Öğretim elemanlarını yükler - ParallelDataLoader kullanır
     */
    private void loadOgretimElemanlari() {
        try {
            logger.info("🚀 Öğretim elemanları ULTRA-FAST PARALEL yükleniyor...");
            
            // Derslerden öğretim elemanı TC'lerini topla
            Set<String> ogretimElemaniTCSet = allDersler.stream()
                .map(Ders::getOgretimElemaniTC)
                .filter(tc -> tc != null && !tc.trim().isEmpty())
                .collect(Collectors.toSet());
            
            logger.info("Toplam {} benzersiz öğretim elemanı TC'si bulundu", ogretimElemaniTCSet.size());
            
            allOgretimElemanlari.clear();
            
            // ParallelDataLoader varsa kullan (çok daha hızlı!)
            if (parallelDataLoader != null) {
                List<OgretimElemani> loaded = parallelDataLoader.loadOgretimElemanlariParallel(
                    new ArrayList<>(ogretimElemaniTCSet)
                );
                allOgretimElemanlari.addAll(loaded);
                logger.info("🎉 ULTRA-FAST yükleme tamamlandı: {} öğretim elemanı", loaded.size());
                return;
            }
            
            // Fallback: Eski yöntem
            logger.info("⚠️ ParallelDataLoader bulunamadı, standart yöntem kullanılıyor");
            allOgretimElemanlari.clear();
            
            // TC'leri paralel olarak işle (10'lu batch'ler halinde)
            List<String> tcList = new ArrayList<>(ogretimElemaniTCSet);
            int batchSize = 10; // 10 paralel SOAP çağrısı
            int totalBatches = (int) Math.ceil((double) tcList.size() / batchSize);
            int totalLoaded = 0;
            
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int startIndex = batchIndex * batchSize;
                int endIndex = Math.min(startIndex + batchSize, tcList.size());
                List<String> batchTCs = tcList.subList(startIndex, endIndex);
                
                logger.info("🔄 Batch {}/{}: {} TC paralel işleniyor...", 
                    batchIndex + 1, totalBatches, batchTCs.size());
                
                // Batch'i paralel işle
                List<CompletableFuture<List<OgretimElemani>>> futures = batchTCs.stream()
                    .map(tc -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String xmlResponse = webServiceClient.getOgretimElemaniByFilters(tc, null, null);
                            List<OgretimElemani> elemanlar = xmlParser.parseOgretimElemanlari(xmlResponse);
                            
                            if (!elemanlar.isEmpty()) {
                                logger.debug("✅ TC {}: {} eleman yüklendi", tc, elemanlar.size());
                            }
                            
                            return elemanlar;
                        } catch (Exception e) {
                            logger.debug("❌ TC {} için hata: {}", tc, e.getMessage());
                            return new ArrayList<OgretimElemani>();
                        }
                    }, soapTaskExecutor))
                    .collect(Collectors.toList());
                
                // Batch sonuçlarını topla
                int batchLoaded = 0;
                for (CompletableFuture<List<OgretimElemani>> future : futures) {
                    try {
                        List<OgretimElemani> elemanlar = future.get(30, TimeUnit.SECONDS); // 30s timeout
                        allOgretimElemanlari.addAll(elemanlar);
                        batchLoaded += elemanlar.size();
                    } catch (Exception e) {
                        logger.warn("Future completion hatası: {}", e.getMessage());
                    }
                }
                
                totalLoaded += batchLoaded;
                logger.info("✅ Batch {}/{} tamamlandı: +{} eleman (Toplam: {})", 
                    batchIndex + 1, totalBatches, batchLoaded, totalLoaded);
                
                // Batch'ler arası rate limiting (SOAP servisini yormamak için)
                if (batchIndex < totalBatches - 1) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(200); // 200ms bekle
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Batch loading kesildi");
                        break;
                    }
                }
            }
            
            logger.info("🎉 PARALEL yükleme tamamlandı: {} öğretim elemanı yüklendi", totalLoaded);
            
        } catch (Exception e) {
            logger.error("Öğretim elemanı yükleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * 🚀 INITIAL LOADING: Sadece kritik dersleri startup'ta yükle (Hızlı başlatım!)
     * Geri kalanı progressive background loading ile yüklenecek
     */
    private void loadSelectedDersOgrencileri() {
        try {
            logger.info("🚀 INITIAL LOADING: Startup'ta {} ders yüklenecek (hızlı başlatım!)", initialCoursesToLoad);
            
            // Sadece ilk N dersi yükle (hızlı startup için)
            List<Ders> initialDersler = allDersler.stream()
                .limit(Math.min(initialCoursesToLoad, allDersler.size()))
                .collect(Collectors.toList());
                
            logger.info("Startup loading: {} ders yüklenecek (rate-limit: {}ms)", 
                initialDersler.size(), rateLimitMs);
            
            // 🚀 INITIAL LOADING: Küçük batch'ler halinde paralel yükle (startup için konservatif)
            int initialBatchSize = 5; // Startup için küçük batch (aggressive değil)
            int totalBatches = (int) Math.ceil((double) initialDersler.size() / initialBatchSize);
            int totalLoaded = 0;
            int totalErrors = 0;
            
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int startIndex = batchIndex * initialBatchSize;
                int endIndex = Math.min(startIndex + initialBatchSize, initialDersler.size());
                List<Ders> batchDersler = initialDersler.subList(startIndex, endIndex);
                
                logger.info("🔄 Initial Batch {}/{}: {} ders paralel yükleniyor...", 
                    batchIndex + 1, totalBatches, batchDersler.size());
                
                // Batch'i paralel işle
                List<CompletableFuture<Map<String, Object>>> futures = batchDersler.stream()
                    .map(ders -> CompletableFuture.supplyAsync(() -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("dersHarId", ders.getDersHarId());
                        result.put("success", false);
                        
                        try {
                            String xmlResponse = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(ders.getDersHarId());
                            List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResponse, ders.getDersHarId());
                            
                            result.put("success", true);
                            result.put("ogrenciler", ogrenciler);
                            result.put("ders", ders);
                            
                            return result;
                        } catch (Exception e) {
                            result.put("error", e.getMessage());
                            logger.debug("Initial loading hatası ({}): {}", ders.getDersHarId(), e.getMessage());
                            return result;
                        }
                    }, soapTaskExecutor))
                    .collect(Collectors.toList());
                
                // Batch sonuçlarını topla
                int batchLoaded = 0;
                int batchErrors = 0;
                for (CompletableFuture<Map<String, Object>> future : futures) {
                    try {
                        Map<String, Object> result = future.get(30, TimeUnit.SECONDS);
                        
                        if ((Boolean) result.get("success")) {
                            @SuppressWarnings("unchecked")
                            List<Ogrenci> ogrenciler = (List<Ogrenci>) result.get("ogrenciler");
                            Ders ders = (Ders) result.get("ders");
                            
                            if (!ogrenciler.isEmpty()) {
                                dersOgrencileriMap.put(ders.getDersHarId(), ogrenciler);
                                batchLoaded++;
                            }
                        } else {
                            batchErrors++;
                        }
                    } catch (Exception e) {
                        batchErrors++;
                        logger.warn("Initial future hatası: {}", e.getMessage());
                    }
                }
                
                totalLoaded += batchLoaded;
                totalErrors += batchErrors;
                
                logger.info("✅ Initial Batch {}/{}: +{} ders, {} hata (Toplam: {}/{})", 
                    batchIndex + 1, totalBatches, batchLoaded, batchErrors, totalLoaded, initialDersler.size());
                
                // Startup için konservatif rate limiting (batch'ler arası)
                if (batchIndex < totalBatches - 1) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(rateLimitMs * 5); // 5x daha konservatif
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Initial loading kesildi: {} ders yüklendi", totalLoaded);
                        break;
                    }
                }
                
                // Çok fazla hata kontrolü
                if (totalErrors > maxErrorsPerBatch * 2) {
                    logger.error("Initial loading'de çok fazla hata ({}), durduruluyor!", totalErrors);
                    break;
                }
            }
            
            int loadedCount = totalLoaded;
            int errorCount = totalErrors;
            
            // Progress tracking güncelle
            totalCoursesProcessed = loadedCount;
            nextBatchStartIndex = initialCoursesToLoad;
            
            logger.info("✅ INITIAL LOADING tamamlandı: {} ders, {} hata", loadedCount, errorCount);
            logger.info("💾 Cache durumu: {} ders-öğrenci mapping", dersOgrencileriMap.size());
            
            if (progressiveLoadingEnabled && nextBatchStartIndex < allDersler.size()) {
                logger.info("🔄 PROGRESSIVE LOADING aktif: Kalan {} ders arka planda yüklenecek", 
                    allDersler.size() - nextBatchStartIndex);
            }
            
            // Index'i yeniden oluştur (yeni verilerle)
            buildOgrenciDerslerIndex();
            
        } catch (Exception e) {
            logger.error("Ders öğrencileri yükleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Hızlı arama için index'leri oluştur
     */
    private void buildIndexes() {
        logger.info("Index'ler oluşturuluyor...");
        
        // Ders index'leri
        dersHarIdIndex.clear();
        fakulteDerslerIndex.clear();
        programDerslerIndex.clear();
        
        for (Ders ders : allDersler) {
            // DersHarId index
            dersHarIdIndex.put(ders.getDersHarId(), ders);
            
            // Fakülte index
            if (ders.getFakAd() != null) {
                fakulteDerslerIndex.computeIfAbsent(ders.getFakAd(), k -> new ArrayList<>()).add(ders);
            }
            
            // Program index
            if (ders.getProgAd() != null) {
                programDerslerIndex.computeIfAbsent(ders.getProgAd(), k -> new ArrayList<>()).add(ders);
            }
        }
        
        // Öğretim elemanı index'leri
        tcKimlikIndex.clear();
        sicilNoIndex.clear();
        fakulteOgretimElemaniIndex.clear();
        
        for (OgretimElemani eleman : allOgretimElemanlari) {
            // TC Kimlik index
            if (eleman.getTcKimlikNo() != null) {
                tcKimlikIndex.put(eleman.getTcKimlikNo(), eleman);
            }
            
            // Sicil No index
            if (eleman.getSicilNo() != null) {
                sicilNoIndex.put(eleman.getSicilNo(), eleman);
            }
            
            // Fakülte index
            if (eleman.getFakAd() != null) {
                fakulteOgretimElemaniIndex.computeIfAbsent(eleman.getFakAd(), k -> new ArrayList<>()).add(eleman);
            }
        }
        
        // 🚀 Öğrenci-Ders index'ini oluştur (cache'teki verilerle)
        ogrenciDerslerIndex.clear();
        buildOgrenciDerslerIndex();
        
        logger.info("Index'ler oluşturuldu - Öğrenci index: {} entry", ogrenciDerslerIndex.size());
    }
    
    /**
     * Cache'teki ders-öğrenci verilerinden öğrenci-ders index'ini oluşturur
     */
    private void buildOgrenciDerslerIndex() {
        logger.info("🚀 Öğrenci-Ders index'i oluşturuluyor...");
        
        for (Map.Entry<String, List<Ogrenci>> entry : dersOgrencileriMap.entrySet()) {
            String dersHarId = entry.getKey();
            List<Ogrenci> ogrenciler = entry.getValue();
            
            // Bu derse ait Ders objesini bul
            Ders ders = dersHarIdIndex.get(dersHarId);
            if (ders == null) continue;
            
            // Her öğrenci için index'e ekle
            for (Ogrenci ogrenci : ogrenciler) {
                if (ogrenci.getOgrNo() != null) {
                    ogrenciDerslerIndex.computeIfAbsent(ogrenci.getOgrNo(), k -> new ArrayList<>()).add(ders);
                }
            }
        }
        
        logger.info("✅ Öğrenci-Ders index'i tamamlandı - {} öğrenci", ogrenciDerslerIndex.size());
    }

    // Getter metodları - Controller'lar bunları kullanacak

    public boolean isInitialized() {
        return isInitialized;
    }

    public List<Ders> getAllDersler() {
        return new ArrayList<>(allDersler);
    }

    public List<OgretimElemani> getAllOgretimElemanlari() {
        return new ArrayList<>(allOgretimElemanlari);
    }

    public Ders getDersByHarId(String dersHarId) {
        return dersHarIdIndex.get(dersHarId);
    }

    public List<Ders> getDerslerByFakulte(String fakulteAdi) {
        return fakulteDerslerIndex.getOrDefault(fakulteAdi, new ArrayList<>());
    }

    public List<Ders> getDerslerByProgram(String programAdi) {
        return programDerslerIndex.getOrDefault(programAdi, new ArrayList<>());
    }

    public OgretimElemani getOgretimElemaniByTC(String tcKimlikNo) {
        return tcKimlikIndex.get(tcKimlikNo);
    }

    public OgretimElemani getOgretimElemaniBySicil(String sicilNo) {
        return sicilNoIndex.get(sicilNo);
    }

    public List<OgretimElemani> getOgretimElemanlariByFakulte(String fakulteAdi) {
        return fakulteOgretimElemaniIndex.getOrDefault(fakulteAdi, new ArrayList<>());
    }

    /**
     * Bölüm bazında öğretim elemanlarını getirir
     */
    public List<OgretimElemani> getOgretimElemanlariByBolum(String bolumAdi) {
        return allOgretimElemanlari.stream()
            .filter(eleman -> eleman.getBolAd() != null && eleman.getBolAd().equalsIgnoreCase(bolumAdi))
            .collect(Collectors.toList());
    }

    /**
     * Ünvan bazında öğretim elemanlarını getirir
     */
    public List<OgretimElemani> getOgretimElemanlariByUnvan(String unvan) {
        return allOgretimElemanlari.stream()
            .filter(eleman -> eleman.getUnvan() != null && eleman.getUnvan().equalsIgnoreCase(unvan))
            .collect(Collectors.toList());
    }

    /**
     * Tüm öğrencileri getirir (cache'teki tüm derslerden)
     */
    public List<Ogrenci> getAllOgrenciler() {
        // Tüm derslerdeki öğrencileri topla ve benzersiz yap (TC Kimlik No bazında)
        Map<String, Ogrenci> uniqueOgrenciler = new HashMap<>();
        
        for (List<Ogrenci> ogrenciler : dersOgrencileriMap.values()) {
            for (Ogrenci ogrenci : ogrenciler) {
                if (ogrenci.getTcKimlikNo() != null && !ogrenci.getTcKimlikNo().isEmpty()) {
                    // TC Kimlik No'ya göre benzersiz tut
                    uniqueOgrenciler.putIfAbsent(ogrenci.getTcKimlikNo(), ogrenci);
                } else if (ogrenci.getOgrNo() != null && !ogrenci.getOgrNo().isEmpty()) {
                    // TC yoksa öğrenci numarasına göre benzersiz tut
                    uniqueOgrenciler.putIfAbsent(ogrenci.getOgrNo(), ogrenci);
                }
            }
        }
        
        return new ArrayList<>(uniqueOgrenciler.values());
    }

    /**
     * Sınıf bazında öğrencileri getirir
     */
    public List<Ogrenci> getOgrencilerBySinif(String sinif) {
        // Tüm derslerdeki öğrencileri topla ve sınıfa göre filtrele
        Map<String, Ogrenci> uniqueOgrenciler = new HashMap<>();
        
        for (List<Ogrenci> ogrenciler : dersOgrencileriMap.values()) {
            for (Ogrenci ogrenci : ogrenciler) {
                if (ogrenci.getSinif() != null && ogrenci.getSinif().equalsIgnoreCase(sinif)) {
                    String key = ogrenci.getTcKimlikNo() != null ? ogrenci.getTcKimlikNo() : ogrenci.getOgrNo();
                    if (key != null) {
                        uniqueOgrenciler.putIfAbsent(key, ogrenci);
                    }
                }
            }
        }
        
        return new ArrayList<>(uniqueOgrenciler.values());
    }

    /**
     * Fakülte bazında öğrencileri getirir
     */
    public List<Ogrenci> getOgrencilerByFakulte(String fakulteAdi) {
        // Tüm derslerdeki öğrencileri topla ve fakülteye göre filtrele
        Map<String, Ogrenci> uniqueOgrenciler = new HashMap<>();
        
        for (List<Ogrenci> ogrenciler : dersOgrencileriMap.values()) {
            for (Ogrenci ogrenci : ogrenciler) {
                if (ogrenci.getFakulte() != null && ogrenci.getFakulte().equalsIgnoreCase(fakulteAdi)) {
                    String key = ogrenci.getTcKimlikNo() != null ? ogrenci.getTcKimlikNo() : ogrenci.getOgrNo();
                    if (key != null) {
                        uniqueOgrenciler.putIfAbsent(key, ogrenci);
                    }
                }
            }
        }
        
        return new ArrayList<>(uniqueOgrenciler.values());
    }

    /**
     * Bölüm bazında öğrencileri getirir
     */
    public List<Ogrenci> getOgrencilerByBolum(String bolumAdi) {
        // Tüm derslerdeki öğrencileri topla ve bölüme göre filtrele
        Map<String, Ogrenci> uniqueOgrenciler = new HashMap<>();
        
        for (List<Ogrenci> ogrenciler : dersOgrencileriMap.values()) {
            for (Ogrenci ogrenci : ogrenciler) {
                if (ogrenci.getBolum() != null && ogrenci.getBolum().equalsIgnoreCase(bolumAdi)) {
                    String key = ogrenci.getTcKimlikNo() != null ? ogrenci.getTcKimlikNo() : ogrenci.getOgrNo();
                    if (key != null) {
                        uniqueOgrenciler.putIfAbsent(key, ogrenci);
                    }
                }
            }
        }
        
        return new ArrayList<>(uniqueOgrenciler.values());
    }

    /**
     * Tüm benzersiz ünvanları getirir
     */
    public Set<String> getAllUnvanlar() {
        return allOgretimElemanlari.stream()
            .map(OgretimElemani::getUnvan)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Tüm benzersiz bölümleri getirir
     */
    public Set<String> getAllBolumler() {
        return allOgretimElemanlari.stream()
            .map(OgretimElemani::getBolAd)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Belirli bir fakülteye ait bölümleri getirir
     */
    public Set<String> getBolumlerByFakulte(String fakulteAdi) {
        // Önce öğretim elemanlarından bölümleri topla
        Set<String> bolumler = allOgretimElemanlari.stream()
            .filter(eleman -> eleman.getFakAd() != null && eleman.getFakAd().equalsIgnoreCase(fakulteAdi))
            .map(OgretimElemani::getBolAd)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        // Derslerden de bölümleri ekle (daha kapsamlı sonuç için)
        Set<String> derslerdenBolumler = allDersler.stream()
            .filter(ders -> ders.getFakAd() != null && ders.getFakAd().equalsIgnoreCase(fakulteAdi))
            .map(Ders::getBolAd)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        bolumler.addAll(derslerdenBolumler);
        
        return bolumler;
    }

    /**
     * Tüm benzersiz sınıfları getirir
     */
    public Set<String> getAllSiniflar() {
        Set<String> siniflar = new HashSet<>();
        for (List<Ogrenci> ogrenciler : dersOgrencileriMap.values()) {
            for (Ogrenci ogrenci : ogrenciler) {
                if (ogrenci.getSinif() != null && !ogrenci.getSinif().isEmpty()) {
                    siniflar.add(ogrenci.getSinif());
                }
            }
        }
        return siniflar;
    }

    public List<Ogrenci> getOgrencilerByDersHarId(String dersHarId) {
        return dersOgrencileriMap.getOrDefault(dersHarId, new ArrayList<>());
    }

    public Set<String> getAllFakulteler() {
        return allDersler.stream()
            .map(Ders::getFakAd)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public Set<String> getAllProgramlar() {
        return allDersler.stream()
            .map(Ders::getProgAd)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Ders ID'sine göre öğretim elemanını cache'ten bulur (HIZLI)
     */
    public OgretimElemani getOgretimElemaniByDersHarId(String dersHarId) {
        if (!isInitialized() || dersHarId == null) {
            return null;
        }
        
        // 1. Dersi bul
        Ders ders = getDersByHarId(dersHarId);
        if (ders == null || ders.getOgretimElemaniTC() == null) {
            logger.debug("Ders {} için öğretim elemanı TC'si bulunamadı", dersHarId);
            return null;
        }
        
        // 2. TC ile öğretim elemanını bul
        OgretimElemani eleman = getOgretimElemaniByTC(ders.getOgretimElemaniTC());
        if (eleman != null) {
            logger.debug("Ders {} için öğretim elemanı bulundu: {}", dersHarId, eleman.getAdSoyad());
        }
        
        return eleman;
    }

    /**
     * 🚀 FAST: Öğrenci numarasına göre aktif dersleri bulur (INDEX-BASED LOOKUP)
     * Performance: O(1) - Çok hızlı!
     */
    public List<Ders> getDerslerByOgrenciNo(String ogrenciNo) {
        if (!isInitialized() || ogrenciNo == null) {
            return new ArrayList<>();
        }
        
        logger.debug("🚀 Öğrenci {} için FAST ders araması başlıyor...", ogrenciNo);
        
        // 1. Index'ten kontrol et (Süper hızlı!)
        if (ogrenciDerslerIndex.containsKey(ogrenciNo)) {
            List<Ders> dersler = ogrenciDerslerIndex.get(ogrenciNo);
            logger.info("✅ Index'ten bulundu - Öğrenci: {}, Ders sayısı: {}", ogrenciNo, dersler.size());
            return new ArrayList<>(dersler); // Defensive copy
        }
        
        // 2. Index'te yoksa lazy load yap
        logger.info("🔄 Index'te yok, lazy loading başlatılıyor: {}", ogrenciNo);
        return getDerslerByOgrenciNoLazy(ogrenciNo);
    }
    
    /**
     * ⚡ EMERGENCY FALLBACK: Background yükleme tamamlanmamışsa kullanılır
     * Artık çok nadir çalışacak çünkü tüm veriler startup'ta yüklü
     */
    private List<Ders> getDerslerByOgrenciNoLazy(String ogrenciNo) {
        logger.warn("⚡ FALLBACK: Background yükleme tamamlanmamış, lazy loading başlıyor: {}", ogrenciNo);
        
        List<Ders> ogrenciDersleri = new ArrayList<>();
        int checkedCount = 0;
        int foundCount = 0;
        int soapCallCount = 0;
        
        // Önce cache'teki dersleri hızlı kontrol et
        for (Ders ders : allDersler) {
            checkedCount++;
            
            if (dersOgrencileriMap.containsKey(ders.getDersHarId())) {
                // Cache'ten süper hızlı kontrol
                List<Ogrenci> ogrenciler = dersOgrencileriMap.get(ders.getDersHarId());
                if (ogrenciler.stream().anyMatch(o -> ogrenciNo.equals(o.getOgrNo()))) {
                    ogrenciDersleri.add(ders);
                    foundCount++;
                }
            } else {
                // Cache'te yok - SOAP'tan çek (artık çok nadir)
                soapCallCount++;
                if (soapCallCount <= 10) { // Max 10 SOAP çağrısı (güvenlik için)
                    try {
                        String xmlResponse = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(ders.getDersHarId());
                        List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResponse, ders.getDersHarId());
                        
                        dersOgrencileriMap.put(ders.getDersHarId(), ogrenciler);
                        
                        // Index'e ekle
                        for (Ogrenci ogrenci : ogrenciler) {
                            ogrenciDerslerIndex.computeIfAbsent(ogrenci.getOgrNo(), k -> new ArrayList<>()).add(ders);
                        }
                        
                        if (ogrenciler.stream().anyMatch(o -> ogrenciNo.equals(o.getOgrNo()))) {
                            ogrenciDersleri.add(ders);
                            foundCount++;
                        }
                        
                        // Kısa rate limit
                        TimeUnit.MILLISECONDS.sleep(25);
                        
                    } catch (Exception e) {
                        logger.debug("Fallback SOAP hatası: {}", e.getMessage());
                    }
                } else {
                    logger.warn("🛑 Çok fazla SOAP çağrısı, kalan dersler atlanıyor");
                    break;
                }
            }
            
            // Progress her 100 derste
            if (checkedCount % 100 == 0) {
                logger.info("🔄 Fallback: {}/{} kontrol, {} bulundu, {} SOAP", 
                    checkedCount, allDersler.size(), foundCount, soapCallCount);
            }
        }
        
        // Sonuç index'e kaydet
        ogrenciDerslerIndex.put(ogrenciNo, new ArrayList<>(ogrenciDersleri));
        
        logger.info("✅ Fallback tamamlandı: {} -> {} ders ({} SOAP çağrısı)", 
            ogrenciNo, foundCount, soapCallCount);
        return ogrenciDersleri;
    }

    /**
     * 🚀 PROGRESSIVE BACKGROUND LOADING: Periyodik olarak kalan dersleri yükler
     * Her 5 dakikada bir çalışır, batch halinde ders yükler
     */
    @Scheduled(fixedRateString = "${cache.progressive.interval-minutes:5}0000") // 5 dakika = 300000ms
    public void progressiveLoadCourses() {
        if (!progressiveLoadingEnabled || !isInitialized || backgroundLoadingComplete) {
            return; // Devre dışı veya tamamlanmış
        }
        
        if (nextBatchStartIndex >= allDersler.size()) {
            backgroundLoadingComplete = true;
            logger.info("🎉 PROGRESSIVE LOADING TAMAMLANDI! Toplam {} ders yüklendi", totalCoursesProcessed);
            return;
        }
        
        logger.info("🔄 Progressive batch başlıyor: Index {}/{}, Batch size: {}", 
            nextBatchStartIndex, allDersler.size(), batchSize);
            
        // Bir sonraki batch'i al
        List<Ders> batchDersler = allDersler.stream()
            .skip(nextBatchStartIndex)
            .limit(batchSize)
            .collect(Collectors.toList());
        
        // 🚀 PARALEL Progressive Loading: Batch'i paralel işle
        List<Ders> toProcess = batchDersler.stream()
            .filter(ders -> !dersOgrencileriMap.containsKey(ders.getDersHarId())) // Cache'te olmayan
            .collect(Collectors.toList());
            
        logger.info("🚀 Progressive PARALEL: {} ders işlenecek", toProcess.size());
        
        // Paralel SOAP çağrıları
        List<CompletableFuture<Map<String, Object>>> futures = toProcess.stream()
            .map(ders -> CompletableFuture.supplyAsync(() -> {
                Map<String, Object> result = new HashMap<>();
                result.put("dersHarId", ders.getDersHarId());
                result.put("success", false);
                result.put("ogrenciler", new ArrayList<Ogrenci>());
                
                try {
                    String xmlResponse = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(ders.getDersHarId());
                    List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResponse, ders.getDersHarId());
                    
                    result.put("success", true);
                    result.put("ogrenciler", ogrenciler);
                    result.put("ders", ders);
                    
                    return result;
                } catch (Exception e) {
                    result.put("error", e.getMessage());
                    logger.debug("Progressive hatası ({}): {}", ders.getDersHarId(), e.getMessage());
                    return result;
                }
            }, soapTaskExecutor))
            .collect(Collectors.toList());
        
        // Sonuçları topla ve cache'e ekle
        int batchLoadedCount = 0;
        int batchErrorCount = 0;
        
        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> result = future.get(45, TimeUnit.SECONDS); // 45s timeout
                
                if ((Boolean) result.get("success")) {
                    @SuppressWarnings("unchecked")
                    List<Ogrenci> ogrenciler = (List<Ogrenci>) result.get("ogrenciler");
                    Ders ders = (Ders) result.get("ders");
                    String dersHarId = (String) result.get("dersHarId");
                    
                    if (!ogrenciler.isEmpty()) {
                        // Thread-safe cache update
                        dersOgrencileriMap.put(dersHarId, ogrenciler);
                        
                        // Öğrenci-ders index'ini güncelle (thread-safe)
                        synchronized (ogrenciDerslerIndex) {
                            for (Ogrenci ogrenci : ogrenciler) {
                                if (ogrenci.getOgrNo() != null) {
                                    ogrenciDerslerIndex.computeIfAbsent(ogrenci.getOgrNo(), k -> new ArrayList<>()).add(ders);
                                }
                            }
                        }
                        
                        batchLoadedCount++;
                        totalCoursesProcessed++;
                    }
                } else {
                    batchErrorCount++;
                }
                
            } catch (Exception e) {
                batchErrorCount++;
                logger.warn("Progressive future hatası: {}", e.getMessage());
            }
        }
        
        // Progressive loading'e özel rate limiting (global seviyede)
        try {
            TimeUnit.MILLISECONDS.sleep(rateLimitMs * 2); // Biraz daha konservatif
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Progressive loading kesildi");
        }
        
        // Progress güncelle
        nextBatchStartIndex += batchSize;
        
        logger.info("✅ Batch tamamlandı: +{} ders, {} hata. Toplam: {}/{}", 
            batchLoadedCount, batchErrorCount, totalCoursesProcessed, allDersler.size());
            
        // Cache status güncelle
        logger.debug("💾 Cache durumu: {} ders-öğrenci, {} öğrenci-ders mapping", 
            dersOgrencileriMap.size(), ogrenciDerslerIndex.size());
    }
    
    /**
     * Progressive loading durumunu döndürür
     */
    public Map<String, Object> getProgressiveLoadingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", progressiveLoadingEnabled);
        status.put("complete", backgroundLoadingComplete);
        status.put("totalCourses", allDersler.size());
        status.put("processedCourses", totalCoursesProcessed);
        status.put("nextBatchIndex", nextBatchStartIndex);
        status.put("batchSize", batchSize);
        status.put("progressPercent", allDersler.isEmpty() ? 0 : (totalCoursesProcessed * 100) / allDersler.size());
        return status;
    }

    /**
     * Cache'i manuel olarak yeniler
     */
    public void refreshCache() {
        logger.info("Cache manuel olarak yenileniyor...");
        
        // Progress tracking'i sıfırla
        totalCoursesProcessed = 0;
        nextBatchStartIndex = 0;
        backgroundLoadingComplete = false;
        
        isInitialized = false;
        initializeCache();
    }
}
