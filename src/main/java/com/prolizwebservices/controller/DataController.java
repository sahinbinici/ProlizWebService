package com.prolizwebservices.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.prolizwebservices.model.Ders;
import com.prolizwebservices.model.Ogrenci;
import com.prolizwebservices.model.OgretimElemani;
import com.prolizwebservices.service.DataCacheService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Cache'lenmiş verilerden filtrelenmiş sonuçlar döndüren controller
 */
@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = {"*"}, 
            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
            allowCredentials = "false")
@Tag(name = "ProlizWebServices", description = "SOAP to REST adapter for Gaziantep University Student Information System")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private DataCacheService cacheService;

    /**
     * 🚀 Cache durumunu kontrol et (Progressive Loading ile)
     */
    @Operation(
        summary = "Get Cache Status with Progressive Loading Info",
        description = "Returns cache status including progressive background loading progress"
    )
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Temel cache bilgileri
        status.put("initialized", cacheService.isInitialized());
        status.put("lastUpdate", cacheService.getLastUpdateTime());
        status.put("totalDersler", cacheService.getAllDersler().size());
        status.put("totalOgretimElemanlari", cacheService.getAllOgretimElemanlari().size());
        status.put("totalFakulteler", cacheService.getAllFakulteler().size());
        status.put("totalProgramlar", cacheService.getAllProgramlar().size());
        
        // 🚀 Progressive loading durumu
        Map<String, Object> progressiveStatus = cacheService.getProgressiveLoadingStatus();
        status.put("progressiveLoading", progressiveStatus);
        
        // Cache efficiency
        int totalCourses = cacheService.getAllDersler().size();
        int cachedCourses = (int) progressiveStatus.get("processedCourses");
        status.put("cacheEfficiency", totalCourses == 0 ? 0 : (cachedCourses * 100) / totalCourses);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Tüm fakülteleri listele
     */
    @Operation(
        summary = "Get All Faculties",
        description = "Returns list of all faculties"
    )
    @GetMapping("/fakulteler")
    public ResponseEntity<Set<String>> getAllFakulteler() {
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build(); // 202 - Cache henüz hazır değil
        }
        
        Set<String> fakulteler = cacheService.getAllFakulteler();
        return ResponseEntity.ok(fakulteler);
    }

    /**
     * Belirtilen fakültenin derslerini listele
     */
    @Operation(
        summary = "Get Courses by Faculty",
        description = "Returns courses for a specific faculty"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Courses found"),
        @ApiResponse(responseCode = "202", description = "Cache not ready yet"),
        @ApiResponse(responseCode = "404", description = "Faculty not found")
    })
    @GetMapping("/fakulte/{fakulteAdi}/dersler")
    public ResponseEntity<List<Ders>> getDerslerByFakulte(
            @Parameter(description = "Faculty name", required = true, example = "MÜHENDİSLİK FAKÜLTESİ")
            @PathVariable String fakulteAdi) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        List<Ders> dersler = cacheService.getDerslerByFakulte(fakulteAdi);
        
        if (dersler.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(dersler);
    }

    /**
     * TC Kimlik No ile öğretim elemanı bul
     */
    @Operation(
        summary = "Get Faculty Member by TC ID",
        description = "Returns faculty member information by TC identification number"
    )
    @GetMapping("/ogretim-elemani/tc/{tcKimlikNo}")
    public ResponseEntity<OgretimElemani> getOgretimElemaniByTC(
            @Parameter(description = "TC identification number", required = true, example = "12345678901")
            @PathVariable String tcKimlikNo) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        OgretimElemani eleman = cacheService.getOgretimElemaniByTC(tcKimlikNo);
        
        if (eleman == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(eleman);
    }

    /**
     * Sicil No ile öğretim elemanı bul
     */
    @Operation(
        summary = "Get Faculty Member by Registry Number",
        description = "Returns faculty member information by registry number"
    )
    @GetMapping("/ogretim-elemani/sicil/{sicilNo}")
    public ResponseEntity<OgretimElemani> getOgretimElemaniBySicil(
            @Parameter(description = "Registry number", required = true, example = "12345")
            @PathVariable String sicilNo) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        OgretimElemani eleman = cacheService.getOgretimElemaniBySicil(sicilNo);
        
        if (eleman == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(eleman);
    }

    /**
     * Ders detayları (ders + öğretim elemanı + öğrenciler)
     */
    @Operation(
        summary = "Get Course Details",
        description = "Returns complete course information including instructor and enrolled students"
    )
    @GetMapping("/ders-detay/{dersHarID}")
    public ResponseEntity<Map<String, Object>> getDersDetay(
            @Parameter(description = "Course ID", required = true, example = "2838793")
            @PathVariable String dersHarID) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        Ders ders = cacheService.getDersByHarId(dersHarID);
        if (ders == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> detay = new HashMap<>();
        detay.put("ders", ders);
        
        // Öğretim elemanını bul
        if (ders.getOgretimElemaniTC() != null) {
            OgretimElemani eleman = cacheService.getOgretimElemaniByTC(ders.getOgretimElemaniTC());
            detay.put("ogretimElemani", eleman);
        }
        
        // Öğrencileri bul
        List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId(dersHarID);
        detay.put("ogrenciler", ogrenciler);
        detay.put("ogrenciSayisi", ogrenciler.size());
        
        return ResponseEntity.ok(detay);
    }

    /**
     * ⚡ HIZLI: Ders ID'sine göre öğretim elemanını cache'ten getir
     */
    @Operation(
        summary = "Get Course Instructor (FAST - from cache)",
        description = "Returns course instructor from cache - much faster than SOAP call"
    )
    @GetMapping("/ders/{dersHarID}/ogretim-elemani-fast")
    public ResponseEntity<OgretimElemani> getOgretimElemaniByDersFast(
            @Parameter(description = "Course ID", required = true, example = "2838793")
            @PathVariable String dersHarID) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        OgretimElemani eleman = cacheService.getOgretimElemaniByDersHarId(dersHarID);
        
        if (eleman == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(eleman);
    }

    /**
     * 🚀 FAST: Öğrenci numarasına göre aktif ders listesini getir (INDEX-BASED)
     */
    @Operation(
        summary = "Get Student's Active Courses (ULTRA FAST)",
        description = "Returns all courses that a student is enrolled in. Uses index-based lookup for lightning-fast performance."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student courses retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"), 
        @ApiResponse(responseCode = "404", description = "Student not found in any course")
    })
    @GetMapping("/ogrenci/{ogrenciNo}/dersler")
    public ResponseEntity<?> getOgrenciDersleri(
            @Parameter(description = "Student Number", required = true, example = "20180001234")
            @PathVariable String ogrenciNo) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        // Öğrenci derslerini bul
        List<Ders> dersler = cacheService.getDerslerByOgrenciNo(ogrenciNo);
        
        if (dersler.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("ogrenciNo", ogrenciNo);
            result.put("dersler", Collections.emptyList());
            result.put("toplamDers", 0);
            result.put("mesaj", "Bu öğrenci numarası için aktif ders bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        // Response oluştur
        Map<String, Object> result = new HashMap<>();
        result.put("ogrenciNo", ogrenciNo);
        result.put("dersler", dersler);
        result.put("toplamDers", dersler.size());
        result.put("mesaj", dersler.size() + " adet aktif ders bulundu");
        
        // Fakülte bazlı grupla
        Map<String, Long> fakulteGrubu = dersler.stream()
            .collect(Collectors.groupingBy(Ders::getFakAd, Collectors.counting()));
        result.put("fakulteBazinda", fakulteGrubu);
        
        // Bolüm bazlı grupla
        Map<String, Long> bolumGrubu = dersler.stream()
            .collect(Collectors.groupingBy(Ders::getBolAd, Collectors.counting()));
        result.put("bolumBazinda", bolumGrubu);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 👤 Öğrenci detaylarını getir (cache'ten)
     */
    @Operation(
        summary = "Get Student Details with Course Info",
        description = "Returns detailed student information including courses and academic statistics"
    )
    @GetMapping("/ogrenci/{ogrenciNo}/detay")
    public ResponseEntity<Map<String, Object>> getOgrenciDetay(
            @Parameter(description = "Student Number", required = true, example = "20180001234")
            @PathVariable String ogrenciNo) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Cache is still loading");
            return ResponseEntity.status(202).body(result);
        }
        
        // Öğrenci derslerini bul
        List<Ders> dersler = cacheService.getDerslerByOgrenciNo(ogrenciNo);
        
        if (dersler.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // İlk dersten öğrenci bilgilerini al (tüm derslerde aynı öğrenci bilgileri var)
        Ogrenci ogrenciOrnek = null;
        for (Ders ders : dersler) {
            List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId(ders.getDersHarId());
            ogrenciOrnek = ogrenciler.stream()
                .filter(o -> ogrenciNo.equals(o.getOgrNo()))
                .findFirst()
                .orElse(null);
            if (ogrenciOrnek != null) break;
        }
        
        // Response oluştur
        Map<String, Object> result = new HashMap<>();
        if (ogrenciOrnek != null) {
            result.put("ogrNo", ogrenciOrnek.getOgrNo());
            result.put("tcKimlikNo", ogrenciOrnek.getTcKimlikNo());
            result.put("adSoyad", ogrenciOrnek.getAdi() + " " + ogrenciOrnek.getSoyadi());
            result.put("fakulte", ogrenciOrnek.getFakulte());
            result.put("bolum", ogrenciOrnek.getBolum());
            result.put("program", ogrenciOrnek.getProgram());
            result.put("sinif", ogrenciOrnek.getSinif());
        }
        
        result.put("aktifDersler", dersler);
        result.put("toplamDersSayisi", dersler.size());
        
        // Kredi hesaplama
        double toplamKredi = dersler.stream()
            .filter(d -> d.getKredi() != null)
            .mapToDouble(d -> {
                try { return Double.parseDouble(d.getKredi()); }
                catch (NumberFormatException e) { return 0.0; }
            })
            .sum();
        result.put("toplamKredi", toplamKredi);
        
        // AKTS hesaplama  
        double toplamAKTS = dersler.stream()
            .filter(d -> d.getAkts() != null)
            .mapToDouble(d -> {
                try { return Double.parseDouble(d.getAkts()); }
                catch (NumberFormatException e) { return 0.0; }
            })
            .sum();
        result.put("toplamAKTS", toplamAKTS);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 🚀 Progressive Loading Durumu
     */
    @Operation(
        summary = "Get Progressive Loading Status",
        description = "Returns detailed progressive background loading status and progress"
    )
    @GetMapping("/cache/progressive-status")
    public ResponseEntity<Map<String, Object>> getProgressiveStatus() {
        Map<String, Object> status = cacheService.getProgressiveLoadingStatus();
        status.put("timestamp", LocalDateTime.now());
        
        // ETA hesaplama (kaba)
        int remaining = (int) status.get("totalCourses") - (int) status.get("processedCourses");
        int batchSize = (int) status.get("batchSize");
        if (remaining > 0 && batchSize > 0) {
            int remainingBatches = (remaining + batchSize - 1) / batchSize; // Ceiling division
            int etaMinutes = remainingBatches * 5; // 5 dakikada bir batch
            status.put("estimatedCompletionMinutes", etaMinutes);
        } else {
            status.put("estimatedCompletionMinutes", 0);
        }
        
        return ResponseEntity.ok(status);
    }

    /**
     * Cache'i manuel olarak yenile
     */
    @Operation(
        summary = "Refresh Cache (with Progressive Loading)",
        description = "Manually refreshes the cache and restarts progressive loading from beginning"
    )
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        
        Map<String, Object> result = new HashMap<>();
        result.put("refreshStarted", LocalDateTime.now());
        
        // Async olarak refresh başlat (geleceğe yönelik @Async kullanılacak)
        // TODO: @Async annotation ile değiştir
        new Thread(() -> {
            try {
                cacheService.refreshCache();
            } catch (Exception e) {
                logger.error("Cache refresh hatası: {}", e.getMessage(), e);
            }
        }).start();
        
        result.put("message", "Cache refresh started with progressive loading");
        result.put("note", "Progressive loading will continue in background after initial load");
        
        return ResponseEntity.ok(result);
    }
}
