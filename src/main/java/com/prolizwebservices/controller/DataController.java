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
@CrossOrigin(origins = {"*"}, 
            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
            allowCredentials = "false")
@Tag(name = "ProlizWebServices", description = "SOAP to REST adapter for Gaziantep University Student Information System")
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
     * 👤 Öğrenci detaylarını getir (cache'ten)
     */
    @Operation(
        summary = "Get Student Details with Course Info",
        description = "Returns detailed student information including courses and academic statistics"
    )
    @GetMapping(value = "/ogrenci/{ogrenciNo}/detay", produces = "application/json")
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
            boolean authenticated = parseSoapBooleanResponse(soapResponse);
            
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
            boolean authenticated = parseSoapBooleanResponse(soapResponse);
            
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
