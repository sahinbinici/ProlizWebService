package com.prolizwebservices.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prolizwebservices.client.OgrenciWebServiceClient;
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
@CrossOrigin(
    origins = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
    allowedHeaders = "*",
    allowCredentials = "false",
    maxAge = 3600
)
@Tag(name = "C-Data Services", description = "Cached data services with advanced filtering and search capabilities")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private DataCacheService cacheService;
    
    @Autowired
    private OgrenciWebServiceClient webServiceClient;

    /**
     * 🚀 Cache durumunu kontrol et (Progressive Loading ile)
     */
    @Operation(
        summary = "Get Cache Status with Progressive Loading Info",
        description = "Returns cache status including progressive background loading progress"
    )
    @GetMapping(value = "/cache/status", produces = "application/json")
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
     * 📄 Tüm dersleri sayfalı şekilde listele (Swagger UI için optimize)
     */
    @Operation(
        summary = "Get All Courses with Pagination",
        description = "Returns paginated list of all courses for better performance"
    )
    @GetMapping(value = "/dersler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getAllDersler(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        List<Ders> allDersler = cacheService.getAllDersler();
        
        // Pagination uygula
        int totalElements = allDersler.size();
        int pageSize = Math.min(size, 100); // Max 100
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);
        
        List<Ders> pagedDersler = Collections.emptyList();
        if (startIndex < totalElements) {
            pagedDersler = allDersler.subList(startIndex, endIndex);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", pagedDersler);
        response.put("page", page);
        response.put("size", pageSize);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / pageSize));
        response.put("first", page == 0);
        response.put("last", endIndex >= totalElements);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Tüm fakülteleri listele
     */
    @Operation(
        summary = "Get All Faculties",
        description = "Returns list of all faculties"
    )
    @GetMapping(value = "/fakulteler", produces = "application/json")
    public ResponseEntity<Set<String>> getAllFakulteler() {
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build(); // 202 - Cache henüz hazır değil
        }
        
        Set<String> fakulteler = cacheService.getAllFakulteler();
        return ResponseEntity.ok(fakulteler);
    }

    /**
     * Belirtilen fakültenin bölümlerini listele
     */
    @Operation(
        summary = "Get Departments by Faculty",
        description = "Returns all departments for a specific faculty"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Departments found"),
        @ApiResponse(responseCode = "202", description = "Cache not ready yet"),
        @ApiResponse(responseCode = "404", description = "Faculty not found or no departments")
    })
    @GetMapping(value = "/fakulte/{fakulteAdi}/bolumler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getBolumlerByFakulte(
            @Parameter(description = "Faculty name", required = true, example = "MÜHENDİSLİK FAKÜLTESİ")
            @PathVariable String fakulteAdi) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        Set<String> bolumler = cacheService.getBolumlerByFakulte(fakulteAdi);
        
        if (bolumler.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("fakulte", fakulteAdi);
            result.put("bolumler", Collections.emptyList());
            result.put("toplamBolum", 0);
            result.put("mesaj", "Bu fakülte için bölüm bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("fakulte", fakulteAdi);
        result.put("bolumler", bolumler);
        result.put("toplamBolum", bolumler.size());
        result.put("mesaj", fakulteAdi + " fakültesinde " + bolumler.size() + " bölüm bulundu");
        
        return ResponseEntity.ok(result);
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
    @GetMapping(value = "/fakulte/{fakulteAdi}/dersler", produces = "application/json")
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
    @GetMapping(value = "/ogretim-elemani/tc/{tcKimlikNo}", produces = "application/json")
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
    @GetMapping(value = "/ogretim-elemani/sicil/{sicilNo}", produces = "application/json")
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
    @GetMapping(value = "/ders-detay/{dersHarID}", produces = "application/json")
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
    @GetMapping(value = "/ders/{dersHarID}/ogretim-elemani-fast", produces = "application/json")
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
    @GetMapping(value = "/ogrenci/{ogrenciNo}/dersler", produces = "application/json")
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
     * 📚 Öğretim elemanının verdiği derslerin listesini getir
     */
    @Operation(
        summary = "Get Courses Taught by Instructor",
        description = "Returns list of courses taught by a specific instructor using sicil number"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Courses retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    @GetMapping(value = "/ogretim-elemani/{sicilNo}/dersler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getDerslerByOgretimElemani(
            @Parameter(description = "Instructor registry number", required = true, example = "12345")
            @PathVariable String sicilNo) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        // 1. Öğretim elemanını bul
        OgretimElemani ogretimElemani = cacheService.getOgretimElemaniBySicil(sicilNo);
        if (ogretimElemani == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Öğretim elemanı bulunamadı");
            result.put("sicilNo", sicilNo);
            return ResponseEntity.status(404).body(result);
        }
        
        // 2. Bu öğretim elemanının verdiği dersleri bul
        List<Ders> tumDersler = cacheService.getAllDersler();
        List<Ders> ogretimElemaniDersleri = tumDersler.stream()
            .filter(ders -> ders.getOgretimElemaniTC() != null && 
                           ders.getOgretimElemaniTC().equals(ogretimElemani.getTcKimlikNo()))
            .collect(Collectors.toList());
        
        // 3. İstatistikler hesapla
        Map<String, Long> fakulteGrubu = ogretimElemaniDersleri.stream()
            .filter(ders -> ders.getFakAd() != null)
            .collect(Collectors.groupingBy(Ders::getFakAd, Collectors.counting()));
            
        Map<String, Long> donemGrubu = ogretimElemaniDersleri.stream()
            .filter(ders -> ders.getDonemAd() != null)
            .collect(Collectors.groupingBy(Ders::getDonemAd, Collectors.counting()));
        
        // 4. Response oluştur
        Map<String, Object> result = new HashMap<>();
        result.put("ogretimElemani", Map.of(
            "sicilNo", ogretimElemani.getSicilNo(),
            "adSoyad", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi(),
            "unvan", ogretimElemani.getUnvan() != null ? ogretimElemani.getUnvan() : "",
            "fakulte", ogretimElemani.getFakAd() != null ? ogretimElemani.getFakAd() : "",
            "bolum", ogretimElemani.getBolAd() != null ? ogretimElemani.getBolAd() : "",
            "ePosta", ogretimElemani.getePosta() != null ? ogretimElemani.getePosta() : ""
        ));
        
        result.put("dersler", ogretimElemaniDersleri);
        result.put("toplamDers", ogretimElemaniDersleri.size());
        result.put("mesaj", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi() + 
                          " öğretim elemanının " + ogretimElemaniDersleri.size() + " adet dersi bulundu");
        
        // İstatistikler
        result.put("istatistikler", Map.of(
            "fakulteBazinda", fakulteGrubu,
            "donemBazinda", donemGrubu
        ));
        
        return ResponseEntity.ok(result);
    }

    /**
     * 📚 TC Kimlik No ile öğretim elemanının verdiği derslerin listesini getir
     */
    @Operation(
        summary = "Get Courses Taught by Instructor (by TC ID)",
        description = "Returns list of courses taught by a specific instructor using TC identification number"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Courses retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "Instructor not found")
    })
    @GetMapping(value = "/ogretim-elemani/tc/{tcKimlikNo}/dersler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getDerslerByOgretimElemaniTC(
            @Parameter(description = "TC identification number", required = true, example = "12345678901")
            @PathVariable String tcKimlikNo) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        // 1. Öğretim elemanını bul
        OgretimElemani ogretimElemani = cacheService.getOgretimElemaniByTC(tcKimlikNo);
        if (ogretimElemani == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Öğretim elemanı bulunamadı");
            result.put("tcKimlikNo", tcKimlikNo);
            return ResponseEntity.status(404).body(result);
        }
        
        // 2. Bu öğretim elemanının verdiği dersleri bul
        List<Ders> tumDersler = cacheService.getAllDersler();
        List<Ders> ogretimElemaniDersleri = tumDersler.stream()
            .filter(ders -> ders.getOgretimElemaniTC() != null && 
                           ders.getOgretimElemaniTC().equals(tcKimlikNo))
            .collect(Collectors.toList());
        
        // 3. İstatistikler hesapla
        Map<String, Long> fakulteGrubu = ogretimElemaniDersleri.stream()
            .filter(ders -> ders.getFakAd() != null)
            .collect(Collectors.groupingBy(Ders::getFakAd, Collectors.counting()));
            
        Map<String, Long> donemGrubu = ogretimElemaniDersleri.stream()
            .filter(ders -> ders.getDonemAd() != null)
            .collect(Collectors.groupingBy(Ders::getDonemAd, Collectors.counting()));
        
        // 4. Response oluştur
        Map<String, Object> result = new HashMap<>();
        result.put("ogretimElemani", Map.of(
            "tcKimlikNo", ogretimElemani.getTcKimlikNo(),
            "sicilNo", ogretimElemani.getSicilNo() != null ? ogretimElemani.getSicilNo() : "",
            "adSoyad", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi(),
            "unvan", ogretimElemani.getUnvan() != null ? ogretimElemani.getUnvan() : "",
            "fakulte", ogretimElemani.getFakAd() != null ? ogretimElemani.getFakAd() : "",
            "bolum", ogretimElemani.getBolAd() != null ? ogretimElemani.getBolAd() : "",
            "ePosta", ogretimElemani.getePosta() != null ? ogretimElemani.getePosta() : ""
        ));
        
        result.put("dersler", ogretimElemaniDersleri);
        result.put("toplamDers", ogretimElemaniDersleri.size());
        result.put("mesaj", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi() + 
                          " öğretim elemanının " + ogretimElemaniDersleri.size() + " adet dersi bulundu");
        
        // İstatistikler
        result.put("istatistikler", Map.of(
            "fakulteBazinda", fakulteGrubu,
            "donemBazinda", donemGrubu
        ));
        
        return ResponseEntity.ok(result);
    }

    /**
     * 👨‍🏫 Öğretim elemanının dersine kayıtlı öğrencileri getir (Yetki kontrolü ile)
     */
    @Operation(
        summary = "Get Students by Instructor and Course (with Authorization)",
        description = "Returns students enrolled in a specific course, verified that the instructor teaches this course"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "403", description = "Instructor does not teach this course"),
        @ApiResponse(responseCode = "404", description = "Course or instructor not found")
    })
    @GetMapping(value = "/ogretim-elemani/tc/{tcKimlikNo}/ders/{dersHarID}/ogrenciler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgrencilerByOgretimElemaniVeDers(
            @Parameter(description = "Instructor TC identification number", required = true, example = "12345678901")
            @PathVariable String tcKimlikNo,
            @Parameter(description = "Course ID", required = true, example = "2838793")
            @PathVariable String dersHarID) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        // 1. Öğretim elemanını bul
        OgretimElemani ogretimElemani = cacheService.getOgretimElemaniByTC(tcKimlikNo);
        if (ogretimElemani == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Öğretim elemanı bulunamadı");
            result.put("tcKimlikNo", tcKimlikNo);
            return ResponseEntity.status(404).body(result);
        }
        
        // 2. Dersi bul
        Ders ders = cacheService.getDersByHarId(dersHarID);
        if (ders == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Ders bulunamadı");
            result.put("dersHarID", dersHarID);
            return ResponseEntity.status(404).body(result);
        }
        
        // 3. Yetki kontrolü: Bu öğretim elemanı bu dersi veriyor mu?
        boolean yetkiliMi = false;
        if (ders.getOgretimElemaniTC() != null && 
            ders.getOgretimElemaniTC().equals(tcKimlikNo)) {
            yetkiliMi = true;
        }
        
        if (!yetkiliMi) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Yetki yok - Bu öğretim elemanı bu dersi vermiyor");
            result.put("tcKimlikNo", tcKimlikNo);
            result.put("dersHarID", dersHarID);
            result.put("ogretimElemani", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi());
            result.put("dersAdi", ders.getDersAdi());
            return ResponseEntity.status(403).body(result);
        }
        
        // 4. Öğrencileri getir
        List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId(dersHarID);
        
        // 5. Response oluştur
        Map<String, Object> result = new HashMap<>();
        result.put("ogretimElemani", Map.of(
            "tcKimlikNo", ogretimElemani.getTcKimlikNo(),
            "sicilNo", ogretimElemani.getSicilNo() != null ? ogretimElemani.getSicilNo() : "",
            "adSoyad", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi(),
            "unvan", ogretimElemani.getUnvan() != null ? ogretimElemani.getUnvan() : "",
            "fakulte", ogretimElemani.getFakAd() != null ? ogretimElemani.getFakAd() : "",
            "bolum", ogretimElemani.getBolAd() != null ? ogretimElemani.getBolAd() : ""
        ));
        
        result.put("ders", Map.of(
            "dersHarID", ders.getDersHarId(),
            "dersKodu", ders.getDersKodu(),
            "dersAdi", ders.getDersAdi(),
            "kredi", ders.getKredi(),
            "akts", ders.getAkts(),
            "donem", ders.getDonemAd()
        ));
        
        result.put("ogrenciler", ogrenciler);
        result.put("toplamOgrenci", ogrenciler.size());
        result.put("mesaj", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi() + 
                          " öğretim elemanının " + ders.getDersAdi() + " dersine kayıtlı " + 
                          ogrenciler.size() + " öğrenci bulundu");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 👤 Öğrenci detaylarını getir (cache'ten) - Geliştirilmiş Versiyon
     */
    @Operation(
        summary = "Get Comprehensive Student Details",
        description = "Returns comprehensive student information including personal info, courses, academic statistics, and enrollment details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student details retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping(value = "/ogrenci/{ogrenciNo}/detay", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgrenciDetay(
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
            result.put("error", "Öğrenci bulunamadı");
            result.put("ogrenciNo", ogrenciNo);
            result.put("mesaj", "Bu öğrenci numarası için kayıt bulunamadı");
            return ResponseEntity.status(404).body(result);
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
        
        // 📋 KİŞİSEL BİLGİLER
        Map<String, Object> kisiselBilgiler = new HashMap<>();
        if (ogrenciOrnek != null) {
            kisiselBilgiler.put("ogrenciNo", ogrenciOrnek.getOgrNo());
            kisiselBilgiler.put("tcKimlikNo", ogrenciOrnek.getTcKimlikNo());
            kisiselBilgiler.put("adi", ogrenciOrnek.getAdi());
            kisiselBilgiler.put("soyadi", ogrenciOrnek.getSoyadi());
            kisiselBilgiler.put("adSoyad", ogrenciOrnek.getAdi() + " " + ogrenciOrnek.getSoyadi());
        }
        result.put("kisiselBilgiler", kisiselBilgiler);
        
        // 🎓 AKADEMİK BİLGİLER
        Map<String, Object> akademikBilgiler = new HashMap<>();
        if (ogrenciOrnek != null) {
            akademikBilgiler.put("fakulte", ogrenciOrnek.getFakulte() != null ? ogrenciOrnek.getFakulte() : "");
            akademikBilgiler.put("bolum", ogrenciOrnek.getBolum() != null ? ogrenciOrnek.getBolum() : "");
            akademikBilgiler.put("program", ogrenciOrnek.getProgram() != null ? ogrenciOrnek.getProgram() : "");
            akademikBilgiler.put("sinif", ogrenciOrnek.getSinif() != null ? ogrenciOrnek.getSinif() : "");
            akademikBilgiler.put("ogrenimDurumu", ogrenciOrnek.getOgrenimDurum() != null ? ogrenciOrnek.getOgrenimDurum() : "");
            akademikBilgiler.put("kayitNedeni", ogrenciOrnek.getKayitNeden() != null ? ogrenciOrnek.getKayitNeden() : "");
        }
        result.put("akademikBilgiler", akademikBilgiler);
        
        // 📚 DERS BİLGİLERİ
        result.put("aktifDersler", dersler);
        result.put("toplamDersSayisi", dersler.size());
        
        // 📊 KREDİ İSTATİSTİKLERİ
        Map<String, Object> krediIstatistikleri = new HashMap<>();
        
        // Toplam Kredi hesaplama
        double toplamKredi = dersler.stream()
            .filter(d -> d.getKredi() != null)
            .mapToDouble(d -> {
                try { return Double.parseDouble(d.getKredi()); }
                catch (NumberFormatException e) { return 0.0; }
            })
            .sum();
        krediIstatistikleri.put("toplamKredi", toplamKredi);
        
        // Toplam AKTS hesaplama  
        double toplamAKTS = dersler.stream()
            .filter(d -> d.getAkts() != null)
            .mapToDouble(d -> {
                try { return Double.parseDouble(d.getAkts()); }
                catch (NumberFormatException e) { return 0.0; }
            })
            .sum();
        krediIstatistikleri.put("toplamAKTS", toplamAKTS);
        
        // Ortalama kredi/ders
        krediIstatistikleri.put("ortalamaKrediPerDers", dersler.size() > 0 ? toplamKredi / dersler.size() : 0);
        krediIstatistikleri.put("ortalamaAKTSPerDers", dersler.size() > 0 ? toplamAKTS / dersler.size() : 0);
        
        result.put("krediIstatistikleri", krediIstatistikleri);
        
        // 📈 DERS İSTATİSTİKLERİ
        Map<String, Object> dersIstatistikleri = new HashMap<>();
        
        // Dönem bazında ders sayısı
        Map<String, Long> donemBazinda = dersler.stream()
            .filter(d -> d.getDonemAd() != null)
            .collect(Collectors.groupingBy(Ders::getDonemAd, Collectors.counting()));
        dersIstatistikleri.put("donemBazinda", donemBazinda);
        
        // Fakülte bazında ders sayısı
        Map<String, Long> fakulteBazinda = dersler.stream()
            .filter(d -> d.getFakAd() != null)
            .collect(Collectors.groupingBy(Ders::getFakAd, Collectors.counting()));
        dersIstatistikleri.put("fakulteBazinda", fakulteBazinda);
        
        // Bölüm bazında ders sayısı
        Map<String, Long> bolumBazinda = dersler.stream()
            .filter(d -> d.getBolAd() != null)
            .collect(Collectors.groupingBy(Ders::getBolAd, Collectors.counting()));
        dersIstatistikleri.put("bolumBazinda", bolumBazinda);
        
        result.put("dersIstatistikleri", dersIstatistikleri);
        
        // 👨‍🏫 ÖĞRETİM ELEMANLARI
        List<Map<String, String>> ogretimElemanlari = new ArrayList<>();
        Set<String> uniqueOgretimElemanlari = new HashSet<>();
        
        for (Ders ders : dersler) {
            if (ders.getOgretimElemaniTC() != null && !uniqueOgretimElemanlari.contains(ders.getOgretimElemaniTC())) {
                uniqueOgretimElemanlari.add(ders.getOgretimElemaniTC());
                OgretimElemani eleman = cacheService.getOgretimElemaniByTC(ders.getOgretimElemaniTC());
                if (eleman != null) {
                    Map<String, String> elemanBilgi = new HashMap<>();
                    elemanBilgi.put("adSoyad", eleman.getAdi() + " " + eleman.getSoyadi());
                    elemanBilgi.put("unvan", eleman.getUnvan() != null ? eleman.getUnvan() : "");
                    elemanBilgi.put("bolum", eleman.getBolAd() != null ? eleman.getBolAd() : "");
                    elemanBilgi.put("ePosta", eleman.getePosta() != null ? eleman.getePosta() : "");
                    ogretimElemanlari.add(elemanBilgi);
                }
            }
        }
        result.put("ogretimElemanlari", ogretimElemanlari);
        result.put("toplamOgretimElemani", ogretimElemanlari.size());
        
        // 📝 ÖZET MESAJ
        if (ogrenciOrnek != null) {
            String mesaj = String.format("%s - %s %s bölümü %s. sınıf öğrencisi. %d aktif dersi bulunmaktadır.",
                ogrenciOrnek.getOgrNo(),
                ogrenciOrnek.getFakulte() != null ? ogrenciOrnek.getFakulte() : "",
                ogrenciOrnek.getBolum() != null ? ogrenciOrnek.getBolum() : "",
                ogrenciOrnek.getSinif() != null ? ogrenciOrnek.getSinif() : "",
                dersler.size()
            );
            result.put("mesaj", mesaj);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 🚀 Progressive Loading Durumu
     */
    @Operation(
        summary = "Get Progressive Loading Status",
        description = "Returns detailed progressive background loading status and progress"
    )
    @GetMapping(value = "/cache/progressive-status", produces = "application/json")
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

    /**
     * 📚 Tüm öğrencileri listele (sayfalı)
     */
    @Operation(
        summary = "Get All Students with Pagination",
        description = "Returns paginated list of all unique students from cached courses"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later")
    })
    @GetMapping(value = "/ogrenciler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getAllOgrenciler(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        List<Ogrenci> allOgrenciler = cacheService.getAllOgrenciler();
        
        // Pagination uygula
        int totalElements = allOgrenciler.size();
        int pageSize = Math.min(size, 100); // Max 100
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);
        
        List<Ogrenci> pagedOgrenciler = Collections.emptyList();
        if (startIndex < totalElements) {
            pagedOgrenciler = allOgrenciler.subList(startIndex, endIndex);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", pagedOgrenciler);
        response.put("page", page);
        response.put("size", pageSize);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / pageSize));
        response.put("first", page == 0);
        response.put("last", endIndex >= totalElements);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 📚 Tüm öğretim elemanlarını listele (sayfalı)
     */
    @Operation(
        summary = "Get All Faculty Members with Pagination",
        description = "Returns paginated list of all faculty members"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Faculty members retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later")
    })
    @GetMapping(value = "/ogretim-elemanlari", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getAllOgretimElemanlari(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        List<OgretimElemani> allOgretimElemanlari = cacheService.getAllOgretimElemanlari();
        
        // Pagination uygula
        int totalElements = allOgretimElemanlari.size();
        int pageSize = Math.min(size, 100); // Max 100
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);
        
        List<OgretimElemani> pagedElemanlar = Collections.emptyList();
        if (startIndex < totalElements) {
            pagedElemanlar = allOgretimElemanlari.subList(startIndex, endIndex);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", pagedElemanlar);
        response.put("page", page);
        response.put("size", pageSize);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / pageSize));
        response.put("first", page == 0);
        response.put("last", endIndex >= totalElements);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 🎓 Sınıf bazında öğrencileri listele
     */
    @Operation(
        summary = "Get Students by Class",
        description = "Returns all students in a specific class"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "No students found for this class")
    })
    @GetMapping(value = "/sinif/{sinif}/ogrenciler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgrencilerBySinif(
            @Parameter(description = "Class name", required = true, example = "1")
            @PathVariable String sinif) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        List<Ogrenci> ogrenciler = cacheService.getOgrencilerBySinif(sinif);
        
        if (ogrenciler.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("sinif", sinif);
            result.put("ogrenciler", Collections.emptyList());
            result.put("toplamOgrenci", 0);
            result.put("mesaj", "Bu sınıf için öğrenci bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("sinif", sinif);
        result.put("ogrenciler", ogrenciler);
        result.put("toplamOgrenci", ogrenciler.size());
        result.put("mesaj", sinif + ". sınıfta " + ogrenciler.size() + " öğrenci bulundu");
        
        // Fakülte bazında grupla
        Map<String, Long> fakulteGrubu = ogrenciler.stream()
            .filter(o -> o.getFakulte() != null)
            .collect(Collectors.groupingBy(Ogrenci::getFakulte, Collectors.counting()));
        result.put("fakulteBazinda", fakulteGrubu);
        
        // Bölüm bazında grupla
        Map<String, Long> bolumGrubu = ogrenciler.stream()
            .filter(o -> o.getBolum() != null)
            .collect(Collectors.groupingBy(Ogrenci::getBolum, Collectors.counting()));
        result.put("bolumBazinda", bolumGrubu);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 🏛️ Fakülte bazında öğrencileri listele
     */
    @Operation(
        summary = "Get Students by Faculty",
        description = "Returns all students in a specific faculty"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "No students found for this faculty")
    })
    @GetMapping(value = "/fakulte/{fakulteAdi}/ogrenciler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgrencilerByFakulte(
            @Parameter(description = "Faculty name", required = true, example = "MÜHENDİSLİK FAKÜLTESİ")
            @PathVariable String fakulteAdi) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        List<Ogrenci> ogrenciler = cacheService.getOgrencilerByFakulte(fakulteAdi);
        
        if (ogrenciler.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("fakulte", fakulteAdi);
            result.put("ogrenciler", Collections.emptyList());
            result.put("toplamOgrenci", 0);
            result.put("mesaj", "Bu fakülte için öğrenci bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("fakulte", fakulteAdi);
        result.put("ogrenciler", ogrenciler);
        result.put("toplamOgrenci", ogrenciler.size());
        result.put("mesaj", fakulteAdi + " fakültesinde " + ogrenciler.size() + " öğrenci bulundu");
        
        // Bölüm bazında grupla
        Map<String, Long> bolumGrubu = ogrenciler.stream()
            .filter(o -> o.getBolum() != null)
            .collect(Collectors.groupingBy(Ogrenci::getBolum, Collectors.counting()));
        result.put("bolumBazinda", bolumGrubu);
        
        // Sınıf bazında grupla
        Map<String, Long> sinifGrubu = ogrenciler.stream()
            .filter(o -> o.getSinif() != null)
            .collect(Collectors.groupingBy(Ogrenci::getSinif, Collectors.counting()));
        result.put("sinifBazinda", sinifGrubu);
        
        // Program bazında grupla
        Map<String, Long> programGrubu = ogrenciler.stream()
            .filter(o -> o.getProgram() != null)
            .collect(Collectors.groupingBy(Ogrenci::getProgram, Collectors.counting()));
        result.put("programBazinda", programGrubu);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 🏢 Bölüm bazında öğrencileri listele
     */
    @Operation(
        summary = "Get Students by Department",
        description = "Returns all students in a specific department"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "No students found for this department")
    })
    @GetMapping(value = "/bolum/{bolumAdi}/ogrenciler", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgrencilerByBolum(
            @Parameter(description = "Department name", required = true, example = "BİLGİSAYAR MÜHENDİSLİĞİ")
            @PathVariable String bolumAdi) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        List<Ogrenci> ogrenciler = cacheService.getOgrencilerByBolum(bolumAdi);
        
        if (ogrenciler.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("bolum", bolumAdi);
            result.put("ogrenciler", Collections.emptyList());
            result.put("toplamOgrenci", 0);
            result.put("mesaj", "Bu bölüm için öğrenci bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("bolum", bolumAdi);
        result.put("ogrenciler", ogrenciler);
        result.put("toplamOgrenci", ogrenciler.size());
        result.put("mesaj", bolumAdi + " bölümünde " + ogrenciler.size() + " öğrenci bulundu");
        
        // Sınıf bazında grupla
        Map<String, Long> sinifGrubu = ogrenciler.stream()
            .filter(o -> o.getSinif() != null)
            .collect(Collectors.groupingBy(Ogrenci::getSinif, Collectors.counting()));
        result.put("sinifBazinda", sinifGrubu);
        
        // Program bazında grupla
        Map<String, Long> programGrubu = ogrenciler.stream()
            .filter(o -> o.getProgram() != null)
            .collect(Collectors.groupingBy(Ogrenci::getProgram, Collectors.counting()));
        result.put("programBazinda", programGrubu);
        
        // Fakülte bilgisi
        String fakulte = ogrenciler.stream()
            .map(Ogrenci::getFakulte)
            .filter(f -> f != null)
            .findFirst()
            .orElse("Bilinmiyor");
        result.put("fakulte", fakulte);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 👨‍🏫 Ünvan bazında öğretim elemanlarını listele
     */
    @Operation(
        summary = "Get Faculty Members by Title",
        description = "Returns all faculty members with a specific academic title"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Faculty members retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "No faculty members found for this title")
    })
    @GetMapping(value = "/unvan/{unvan}/ogretim-elemanlari", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgretimElemanlariByUnvan(
            @Parameter(description = "Academic title", required = true, example = "Prof. Dr.")
            @PathVariable String unvan) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        List<OgretimElemani> elemanlar = cacheService.getOgretimElemanlariByUnvan(unvan);
        
        if (elemanlar.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("unvan", unvan);
            result.put("ogretimElemanlari", Collections.emptyList());
            result.put("toplamEleman", 0);
            result.put("mesaj", "Bu ünvan için öğretim elemanı bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("unvan", unvan);
        result.put("ogretimElemanlari", elemanlar);
        result.put("toplamEleman", elemanlar.size());
        result.put("mesaj", unvan + " ünvanında " + elemanlar.size() + " öğretim elemanı bulundu");
        
        // Fakülte bazında grupla
        Map<String, Long> fakulteGrubu = elemanlar.stream()
            .filter(e -> e.getFakAd() != null)
            .collect(Collectors.groupingBy(OgretimElemani::getFakAd, Collectors.counting()));
        result.put("fakulteBazinda", fakulteGrubu);
        
        // Bölüm bazında grupla
        Map<String, Long> bolumGrubu = elemanlar.stream()
            .filter(e -> e.getBolAd() != null)
            .collect(Collectors.groupingBy(OgretimElemani::getBolAd, Collectors.counting()));
        result.put("bolumBazinda", bolumGrubu);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 🏢 Bölüm bazında öğretim elemanlarını listele
     */
    @Operation(
        summary = "Get Faculty Members by Department",
        description = "Returns all faculty members in a specific department"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Faculty members retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "No faculty members found for this department")
    })
    @GetMapping(value = "/bolum/{bolumAdi}/ogretim-elemanlari", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgretimElemanlariByBolum(
            @Parameter(description = "Department name", required = true, example = "BİLGİSAYAR MÜHENDİSLİĞİ")
            @PathVariable String bolumAdi) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        List<OgretimElemani> elemanlar = cacheService.getOgretimElemanlariByBolum(bolumAdi);
        
        if (elemanlar.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("bolum", bolumAdi);
            result.put("ogretimElemanlari", Collections.emptyList());
            result.put("toplamEleman", 0);
            result.put("mesaj", "Bu bölüm için öğretim elemanı bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("bolum", bolumAdi);
        result.put("ogretimElemanlari", elemanlar);
        result.put("toplamEleman", elemanlar.size());
        result.put("mesaj", bolumAdi + " bölümünde " + elemanlar.size() + " öğretim elemanı bulundu");
        
        // Ünvan bazında grupla
        Map<String, Long> unvanGrubu = elemanlar.stream()
            .filter(e -> e.getUnvan() != null)
            .collect(Collectors.groupingBy(OgretimElemani::getUnvan, Collectors.counting()));
        result.put("unvanBazinda", unvanGrubu);
        
        // Fakülte bilgisi
        String fakulte = elemanlar.stream()
            .map(OgretimElemani::getFakAd)
            .filter(f -> f != null)
            .findFirst()
            .orElse("Bilinmiyor");
        result.put("fakulte", fakulte);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 🏛️ Fakülte bazında öğretim elemanlarını listele
     */
    @Operation(
        summary = "Get Faculty Members by Faculty",
        description = "Returns all faculty members in a specific faculty"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Faculty members retrieved successfully"),
        @ApiResponse(responseCode = "202", description = "Cache still loading, try again later"),
        @ApiResponse(responseCode = "404", description = "No faculty members found for this faculty")
    })
    @GetMapping(value = "/fakulte/{fakulteAdi}/ogretim-elemanlari", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getOgretimElemanlariByFakulte(
            @Parameter(description = "Faculty name", required = true, example = "MÜHENDİSLİK FAKÜLTESİ")
            @PathVariable String fakulteAdi) {
        
        if (!cacheService.isInitialized()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "CACHE_LOADING");
            result.put("message", "Cache is still loading, please try again in a few moments");
            return ResponseEntity.status(202).body(result);
        }
        
        List<OgretimElemani> elemanlar = cacheService.getOgretimElemanlariByFakulte(fakulteAdi);
        
        if (elemanlar.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("fakulte", fakulteAdi);
            result.put("ogretimElemanlari", Collections.emptyList());
            result.put("toplamEleman", 0);
            result.put("mesaj", "Bu fakülte için öğretim elemanı bulunamadı");
            return ResponseEntity.ok(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("fakulte", fakulteAdi);
        result.put("ogretimElemanlari", elemanlar);
        result.put("toplamEleman", elemanlar.size());
        result.put("mesaj", fakulteAdi + " fakültesinde " + elemanlar.size() + " öğretim elemanı bulundu");
        
        // Ünvan bazında grupla
        Map<String, Long> unvanGrubu = elemanlar.stream()
            .filter(e -> e.getUnvan() != null)
            .collect(Collectors.groupingBy(OgretimElemani::getUnvan, Collectors.counting()));
        result.put("unvanBazinda", unvanGrubu);
        
        // Bölüm bazında grupla
        Map<String, Long> bolumGrubu = elemanlar.stream()
            .filter(e -> e.getBolAd() != null)
            .collect(Collectors.groupingBy(OgretimElemani::getBolAd, Collectors.counting()));
        result.put("bolumBazinda", bolumGrubu);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 📋 Tüm sınıfları listele
     */
    @Operation(
        summary = "Get All Classes",
        description = "Returns list of all unique class names"
    )
    @GetMapping(value = "/siniflar", produces = "application/json")
    public ResponseEntity<Set<String>> getAllSiniflar() {
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        Set<String> siniflar = cacheService.getAllSiniflar();
        return ResponseEntity.ok(siniflar);
    }

    /**
     * 📋 Tüm ünvanları listele
     */
    @Operation(
        summary = "Get All Academic Titles",
        description = "Returns list of all unique academic titles"
    )
    @GetMapping(value = "/unvanlar", produces = "application/json")
    public ResponseEntity<Set<String>> getAllUnvanlar() {
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        Set<String> unvanlar = cacheService.getAllUnvanlar();
        return ResponseEntity.ok(unvanlar);
    }

    /**
     * 📋 Tüm bölümleri listele
     */
    @Operation(
        summary = "Get All Departments",
        description = "Returns list of all unique department names"
    )
    @GetMapping(value = "/bolumler", produces = "application/json")
    public ResponseEntity<Set<String>> getAllBolumler() {
        if (!cacheService.isInitialized()) {
            return ResponseEntity.accepted().build();
        }
        
        Set<String> bolumler = cacheService.getAllBolumler();
        return ResponseEntity.ok(bolumler);
    }

    /**
     * 🔐 Akademik personel şifre kontrolü
     */
    @Operation(
        summary = "Academic Staff Password Verification",
        description = "Verifies academic staff credentials (registry number and password). Password is automatically hashed with MD5."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "500", description = "Service error")
    })
    @PostMapping(value = "/auth/akademik-personel", produces = "application/json")
    public ResponseEntity<Map<String, Object>> akademikPersonelSifreKontrol(
            @Parameter(description = "Registry Number (Sicil No)", required = true, example = "12345")
            @RequestParam String sicilNo,
            @Parameter(description = "Password (will be MD5 hashed)", required = true)
            @RequestParam String sifre) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Akademik personel şifre kontrolü başlatıldı - Sicil No: {}", sicilNo);
            
            // Web servis çağrısı
            String soapResponse = webServiceClient.akademikPersonelSifreKontrol(sicilNo, sifre);
            
            // SOAP yanıtını parse et
            boolean authenticated = webServiceClient.parseSoapResponse(soapResponse);
            
            if (authenticated) {
                result.put("success", true);
                result.put("message", "Giriş başarılı");
                result.put("sicilNo", sicilNo);
                result.put("timestamp", LocalDateTime.now());
                
                // Öğretim elemanı detaylarını ekle (cache'ten)
                if (cacheService.isInitialized()) {
                    OgretimElemani ogretimElemani = cacheService.getOgretimElemaniBySicil(sicilNo);
                    if (ogretimElemani != null) {
                        result.put("adSoyad", ogretimElemani.getAdi() + " " + ogretimElemani.getSoyadi());
                        result.put("unvan", ogretimElemani.getUnvan());
                        result.put("fakulte", ogretimElemani.getFakAd());
                        result.put("bolum", ogretimElemani.getBolAd());
                        result.put("ePosta", ogretimElemani.getePosta());
                    }
                }
                
                logger.info("Akademik personel girişi başarılı - Sicil No: {}", sicilNo);
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "Sicil numarası veya şifre hatalı");
                logger.warn("Akademik personel girişi başarısız - Sicil No: {}", sicilNo);
                return ResponseEntity.status(401).body(result);
            }
            
        } catch (Exception e) {
            logger.error("Akademik personel şifre kontrolü hatası - Sicil No: {}, Hata: {}", 
                        sicilNo, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Servis hatası: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 🔐 Öğrenci giriş kontrolü
     */
    @Operation(
        summary = "Student Login Verification",
        description = "Verifies student credentials (student number and password). Password is automatically hashed with MD5."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "500", description = "Service error")
    })
    @PostMapping(value = "/auth/ogrenci", produces = "application/json")
    public ResponseEntity<Map<String, Object>> ogrenciGirisKontrol(
            @Parameter(description = "Student Number", required = true, example = "20180001234")
            @RequestParam String ogrenciNo,
            @Parameter(description = "Password (will be MD5 hashed)", required = true)
            @RequestParam String sifre) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Öğrenci giriş kontrolü başlatıldı - Öğrenci: {}", ogrenciNo);
            
            // Web servis çağrısı (şifre otomatik olarak MD5 hash'lenir)
            String soapResponse = webServiceClient.ogrenciSifreKontrol(ogrenciNo, sifre);
            
            // SOAP yanıtını parse et
            boolean authenticated = webServiceClient.parseSoapResponse(soapResponse);
            
            if (authenticated) {
                result.put("success", true);
                result.put("message", "Giriş başarılı");
                result.put("ogrenciNo", ogrenciNo);
                result.put("timestamp", LocalDateTime.now());
                
                // Öğrenci detaylarını ekle (cache'ten)
                if (cacheService.isInitialized()) {
                    List<Ders> dersler = cacheService.getDerslerByOgrenciNo(ogrenciNo);
                    if (!dersler.isEmpty()) {
                        // İlk dersten öğrenci bilgilerini al
                        for (Ders ders : dersler) {
                            List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId(ders.getDersHarId());
                            Ogrenci ogrenci = ogrenciler.stream()
                                .filter(o -> ogrenciNo.equals(o.getOgrNo()))
                                .findFirst()
                                .orElse(null);
                            if (ogrenci != null) {
                                result.put("adSoyad", ogrenci.getAdi() + " " + ogrenci.getSoyadi());
                                result.put("fakulte", ogrenci.getFakulte());
                                result.put("bolum", ogrenci.getBolum());
                                result.put("program", ogrenci.getProgram());
                                result.put("sinif", ogrenci.getSinif());
                                break;
                            }
                        }
                    }
                }
                
                logger.info("Öğrenci girişi başarılı - Öğrenci: {}", ogrenciNo);
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "Öğrenci numarası veya şifre hatalı");
                logger.warn("Öğrenci girişi başarısız - Öğrenci: {}", ogrenciNo);
                return ResponseEntity.status(401).body(result);
            }
            
        } catch (Exception e) {
            logger.error("Öğrenci giriş kontrolü hatası - Öğrenci: {}, Hata: {}", 
                        ogrenciNo, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Servis hatası: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * SOAP yanıtından boolean değer parse eder
     */
    private boolean parseSoapBooleanResponse(String soapResponse) {
        if (soapResponse == null || soapResponse.isEmpty()) {
            return false;
        }
        
        // SOAP yanıtında "true" veya "1" varsa başarılı
        // Not: WSDL'de "Sucess" yazıyor (typo), "Success" değil
        String lowerResponse = soapResponse.toLowerCase();
        return lowerResponse.contains("<result>true</result>") || 
               lowerResponse.contains("<result>1</result>") ||
               lowerResponse.contains("<sucess>true</sucess>") ||  // WSDL typo
               lowerResponse.contains("<success>true</success>") ||
               lowerResponse.contains(">true<") ||
               lowerResponse.contains(">1<");
    }
}
