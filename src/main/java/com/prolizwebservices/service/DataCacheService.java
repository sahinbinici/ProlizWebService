package com.prolizwebservices.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     */
    @PostConstruct
    public void initializeCache() {
        logger.info("DataCache initialize ediliyor...");
        
        try {
            // 1. Dersleri yükle
            loadDersler();
            
            // 2. Öğretim elemanlarını yükle (tüm fakülteler için)
            loadOgretimElemanlari();
            
            // 3. Index'leri oluştur
            buildIndexes();
            
            // 4. Ders öğrencilerini yükle (seçili dersler için)
            loadSelectedDersOgrencileri();
            
            lastUpdateTime = LocalDateTime.now();
            isInitialized = true;
            
            logger.info("DataCache initialize tamamlandı - Dersler: {}, Öğretim Elemanları: {}, Ders-Öğrenci: {}", 
                allDersler.size(), allOgretimElemanlari.size(), dersOgrencileriMap.size());
                
        } catch (Exception e) {
            logger.error("DataCache initialize hatası: {}", e.getMessage(), e);
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
     * Öğretim elemanlarını yükler - Derslerden TC'leri toplayıp her birini çağırır
     */
    private void loadOgretimElemanlari() {
        try {
            logger.info("Öğretim elemanları yükleniyor...");
            
            // Derslerden öğretim elemanı TC'lerini topla
            Set<String> ogretimElemaniTCSet = allDersler.stream()
                .map(Ders::getOgretimElemaniTC)
                .filter(tc -> tc != null && !tc.trim().isEmpty())
                .collect(Collectors.toSet());
            
            logger.info("Toplam {} benzersiz öğretim elemanı TC'si bulundu", ogretimElemaniTCSet.size());
            
            allOgretimElemanlari.clear();
            
            // Her TC için öğretim elemanı bilgilerini al
            int loadedCount = 0;
            for (String tc : ogretimElemaniTCSet) {
                try {
                    String xmlResponse = webServiceClient.getOgretimElemaniByFilters(tc, null, null);
                    List<OgretimElemani> elemanlar = xmlParser.parseOgretimElemanlari(xmlResponse);
                    
                    allOgretimElemanlari.addAll(elemanlar);
                    loadedCount += elemanlar.size();
                    
                    // Rate limiting - çok hızlı istek göndermeyelim
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Öğretim elemanı yükleme işlemi kesildi");
                        break;
                    }
                    
                } catch (Exception e) {
                    logger.debug("TC {} için öğretim elemanı yükleme hatası: {}", tc, e.getMessage());
                }
            }
            
            logger.info("{} öğretim elemanı başarıyla yüklendi", loadedCount);
            
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
            
            int loadedCount = 0;
            int errorCount = 0;
            
            for (Ders ders : initialDersler) {
                try {
                    String xmlResponse = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(ders.getDersHarId());
                    List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResponse, ders.getDersHarId());
                    
                    if (!ogrenciler.isEmpty()) {
                        dersOgrencileriMap.put(ders.getDersHarId(), ogrenciler);
                        loadedCount++;
                    }
                    
                    // Progress log (her 25 derste - az ders olduğu için)
                    if (loadedCount % 25 == 0 && loadedCount > 0) {
                        logger.info("🔄 Initial Progress: {}/{} ders yüklendi, {} hata", 
                            loadedCount, initialDersler.size(), errorCount);
                    }
                    
                    // Hızlı rate limiting (startup için)
                    try {
                        TimeUnit.MILLISECONDS.sleep(rateLimitMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Initial loading kesildi: {} ders yüklendi", loadedCount);
                        break;
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    if (errorCount <= 5) { // İlk 5 hatayı detaylı logla
                        logger.warn("Initial loading hatası ({}): {}", ders.getDersHarId(), e.getMessage());
                    } else {
                        logger.debug("Ders {} initial loading hatası: {}", ders.getDersHarId(), e.getMessage());
                    }
                    
                    // Startup'ta daha toleranslı olalım
                    if (errorCount > maxErrorsPerBatch * 2) {
                        logger.error("Initial loading'de çok fazla hata ({}), durduruluyor!", errorCount);
                        break;
                    }
                }
            }
            
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
        
        int batchLoadedCount = 0;
        int batchErrorCount = 0;
        
        for (Ders ders : batchDersler) {
            // Zaten cache'te var mı kontrol et
            if (dersOgrencileriMap.containsKey(ders.getDersHarId())) {
                continue; // Skip - zaten var
            }
            
            try {
                String xmlResponse = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(ders.getDersHarId());
                List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResponse, ders.getDersHarId());
                
                if (!ogrenciler.isEmpty()) {
                    dersOgrencileriMap.put(ders.getDersHarId(), ogrenciler);
                    
                    // Öğrenci-ders index'ini güncelle
                    for (Ogrenci ogrenci : ogrenciler) {
                        if (ogrenci.getOgrNo() != null) {
                            ogrenciDerslerIndex.computeIfAbsent(ogrenci.getOgrNo(), k -> new ArrayList<>()).add(ders);
                        }
                    }
                    
                    batchLoadedCount++;
                    totalCoursesProcessed++;
                }
                
                // Progressive rate limiting
                try {
                    TimeUnit.MILLISECONDS.sleep(rateLimitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Progressive loading kesildi");
                    break;
                }
                
            } catch (Exception e) {
                batchErrorCount++;
                logger.debug("Progressive loading hatası ({}): {}", ders.getDersHarId(), e.getMessage());
                
                if (batchErrorCount > maxErrorsPerBatch) {
                    logger.warn("Batch'te çok fazla hata ({}), batch atlanıyor", batchErrorCount);
                    break;
                }
            }
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
