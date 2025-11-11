package com.prolizwebservices.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prolizwebservices.client.GauExtWebServiceClient;
import com.prolizwebservices.exception.ValidationException;
import com.prolizwebservices.model.OgrenciIstatistik;
import com.prolizwebservices.model.AktifOgrenci;
import com.prolizwebservices.util.XmlParser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/gau-ext")
@CrossOrigin(
    origins = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
    allowedHeaders = "*",
    allowCredentials = "false",
    maxAge = 3600
)
@Tag(name = "A-GAU External Services", description = "Gaziantep University External SOAP Services Integration")
public class GauExtController {

    private final GauExtWebServiceClient webServiceClient;
    private final XmlParser xmlParser;

    @Autowired
    public GauExtController(GauExtWebServiceClient webServiceClient, XmlParser xmlParser) {
        this.webServiceClient = webServiceClient;
        this.xmlParser = xmlParser;
    }
    
    // 1. Student Statistics
    @Operation(
        summary = "Get Student Statistics",
        description = "Retrieves comprehensive statistics about students, academic staff, and organizational units."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "Remote SOAP service unavailable")
    })
    @GetMapping("/istatistik/ogrenci")
    public ResponseEntity<Map<String, Object>> getOgrenciIstatistik() {
        
        String xmlResult = webServiceClient.getOgrenciIstatistik();
        List<OgrenciIstatistik> istatistikler = xmlParser.parseOgrenciIstatistik(xmlResult);
        
        Map<String, Object> response = new HashMap<>();
        response.put("istatistikler", istatistikler);
        response.put("toplamKayit", istatistikler.size());
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("message", "Öğrenci istatistikleri başarıyla getirildi");
        
        return ResponseEntity.ok(response);
    }

    // 2. Active Student List
    @Operation(
        summary = "Get Active Student List",
        description = "Retrieves detailed information about active students. Can filter by student number or TC identity number."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student list retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "Remote SOAP service unavailable")
    })
    @GetMapping("/ogrenci/aktif-liste")
    public ResponseEntity<Map<String, Object>> getAktifOgrenciListesi(
            @Parameter(description = "Student number (optional)", example = "20230001")
            @RequestParam(required = false) String ogrenciNo,
            @Parameter(description = "TC identity number (optional)", example = "12345678901")
            @RequestParam(required = false) String tcKimlik,
            @Parameter(description = "Maximum number of students to return (default: 50, max: 100)", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        
        String xmlResult = webServiceClient.getAktifOgrenciListesi(ogrenciNo, tcKimlik);
        List<AktifOgrenci> allOgrenciler = xmlParser.parseAktifOgrenciler(xmlResult);
        
        // Limit uygula (Swagger UI performansı için)
        List<AktifOgrenci> limitedOgrenciler = allOgrenciler.stream()
            .limit(Math.min(limit, 100)) // Max 100 ile sınırla
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("ogrenciler", limitedOgrenciler);
        response.put("toplamOgrenci", allOgrenciler.size());
        response.put("gosterilenOgrenci", limitedOgrenciler.size());
        response.put("filtreler", Map.of(
            "ogrenciNo", ogrenciNo != null ? ogrenciNo : "",
            "tcKimlik", tcKimlik != null ? tcKimlik : ""
        ));
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("message", limitedOgrenciler.size() > 0 ? 
            "Aktif öğrenci listesi başarıyla getirildi" : "Kriterlere uygun öğrenci bulunamadı");
        
        return ResponseEntity.ok(response);
    }

    // 3. Academic Staff Authentication (GAU EXT)
    @Operation(
        summary = "Academic Staff Password Verification (GAU EXT)",
        description = "Authenticates academic staff members using GAU External service. Password is automatically hashed with MD5."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication request processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "503", description = "Remote SOAP service unavailable")
    })
    @PostMapping(value = "/akademik-personel/sifre-kontrol", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> akademikPersonelSifreKontrol(
            @Parameter(description = "Academic staff username (Sicil No)", required = true, example = "78555023")
            @RequestParam String sicilNo,
            @Parameter(description = "Academic staff password (will be MD5 hashed)", required = true, example = "password123")
            @RequestParam String sifre,
            @Parameter(description = "Client IP address (optional)", example = "192.168.1.100")
            @RequestParam(required = false) String ipAddress) {
        
        validateNotEmpty(sicilNo, "sicilNo");
        validateNotEmpty(sifre, "sifre");
        
        String result = webServiceClient.akademikPersonelSifreKontrol(sicilNo, sifre, ipAddress);
        boolean success = webServiceClient.parseSoapResponse(result);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? 
            "Akademik personel girişi başarılı" : "Sicil numarası veya şifre hatalı");
        response.put("sicilNo", sicilNo);
        response.put("ipAddress", ipAddress);
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("service", "GAU-EXT");
        
        return ResponseEntity.ok(response);
    }

    // 4. Student Authentication (GAU EXT)
    @Operation(
        summary = "Student Password Verification (GAU EXT)",
        description = "Authenticates students using GAU External service. Password is automatically hashed with MD5."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication request processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "503", description = "Remote SOAP service unavailable")
    })
    @PostMapping(value = "/ogrenci/sifre-kontrol", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ogrenciSifreKontrol(
            @Parameter(description = "Student number", required = true, example = "20230001")
            @RequestParam String ogrenciNo,
            @Parameter(description = "Student password (will be MD5 hashed)", required = true, example = "password123")
            @RequestParam String sifre,
            @Parameter(description = "Client IP address (optional)", example = "192.168.1.100")
            @RequestParam(required = false) String ipAddress) {
        
        validateNotEmpty(ogrenciNo, "ogrenciNo");
        validateNotEmpty(sifre, "sifre");
        
        String result = webServiceClient.ogrenciGirisKontrol(ogrenciNo, sifre, ipAddress);
        boolean success = webServiceClient.parseSoapResponse(result);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? 
            "Öğrenci girişi başarılı" : "Öğrenci numarası veya şifre hatalı");
        response.put("ogrenciNo", ogrenciNo);
        response.put("ipAddress", ipAddress);
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("service", "GAU-EXT");
        
        return ResponseEntity.ok(response);
    }

    // 5. Get Students by Advisor (Danışman bazlı öğrenci listesi)
    @Operation(
        summary = "Get Students by Advisor",
        description = "Retrieves list of students for a specific advisor from cached data. Filters by advisor name and surname."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Student list retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "503", description = "Cache not initialized")
    })
    @GetMapping("/ogrenci/danisman-ogrencileri")
    public ResponseEntity<Map<String, Object>> getDanismanOgrencileri(
            @Parameter(description = "Advisor name (optional)", example = "Mehmet")
            @RequestParam(required = false) String danismanAd,
            @Parameter(description = "Advisor surname (optional)", example = "Kaya")
            @RequestParam(required = false) String danismanSoyad,
            @Parameter(description = "Full name search (optional)", example = "Mehmet Kaya")
            @RequestParam(required = false) String danismanAdSoyad) {
        
        // En az bir parametre gerekli
        if (!StringUtils.hasText(danismanAd) && !StringUtils.hasText(danismanSoyad) && !StringUtils.hasText(danismanAdSoyad)) {
            throw new ValidationException(
                "At least one parameter (danismanAd, danismanSoyad, or danismanAdSoyad) is required",
                "danismanAd/danismanSoyad/danismanAdSoyad",
                null
            );
        }
        
        // Tüm aktif öğrencileri çek
        String xmlResult = webServiceClient.getAktifOgrenciListesi(null, null);
        List<AktifOgrenci> allOgrenciler = xmlParser.parseAktifOgrenciler(xmlResult);
        
        // Danışman filtreleme
        List<AktifOgrenci> filteredOgrenciler = allOgrenciler.stream()
            .filter(ogrenci -> {
                // Tam ad araması varsa
                if (StringUtils.hasText(danismanAdSoyad)) {
                    String fullName = (ogrenci.getDanismanAd() + " " + ogrenci.getDanismanSoyad()).trim().toLowerCase();
                    return fullName.contains(danismanAdSoyad.toLowerCase());
                }
                
                // Ad ve/veya soyad araması
                boolean adMatch = true;
                boolean soyadMatch = true;
                
                if (StringUtils.hasText(danismanAd)) {
                    adMatch = ogrenci.getDanismanAd() != null && 
                             ogrenci.getDanismanAd().toLowerCase().contains(danismanAd.toLowerCase());
                }
                
                if (StringUtils.hasText(danismanSoyad)) {
                    soyadMatch = ogrenci.getDanismanSoyad() != null && 
                                ogrenci.getDanismanSoyad().toLowerCase().contains(danismanSoyad.toLowerCase());
                }
                
                return adMatch && soyadMatch;
            })
            .collect(Collectors.toList());
        
        // Danışman bilgilerini grupla (benzersiz danışmanlar)
        Map<String, Map<String, Object>> danismanlar = new HashMap<>();
        for (AktifOgrenci ogrenci : filteredOgrenciler) {
            String danismanKey = (ogrenci.getDanismanAd() + " " + ogrenci.getDanismanSoyad()).trim();
            if (!danismanKey.isEmpty()) {
                danismanlar.putIfAbsent(danismanKey, Map.of(
                    "danismanAd", ogrenci.getDanismanAd() != null ? ogrenci.getDanismanAd() : "",
                    "danismanSoyad", ogrenci.getDanismanSoyad() != null ? ogrenci.getDanismanSoyad() : "",
                    "danismanUnvan", ogrenci.getDanismanUnvan() != null ? ogrenci.getDanismanUnvan() : ""
                ));
            }
        }
        
        // İstatistikler
        Map<String, Long> fakulteIstatistik = filteredOgrenciler.stream()
            .filter(o -> o.getFakulteAd() != null)
            .collect(Collectors.groupingBy(AktifOgrenci::getFakulteAd, Collectors.counting()));
            
        Map<String, Long> bolumIstatistik = filteredOgrenciler.stream()
            .filter(o -> o.getBolumAd() != null)
            .collect(Collectors.groupingBy(AktifOgrenci::getBolumAd, Collectors.counting()));
            
        Map<String, Long> sinifIstatistik = filteredOgrenciler.stream()
            .filter(o -> o.getSinif() != null)
            .collect(Collectors.groupingBy(AktifOgrenci::getSinif, Collectors.counting()));
        
        Map<String, Object> response = new HashMap<>();
        response.put("ogrenciler", filteredOgrenciler);
        response.put("toplamOgrenci", filteredOgrenciler.size());
        response.put("danismanlar", danismanlar.values());
        response.put("danismanSayisi", danismanlar.size());
        response.put("istatistikler", Map.of(
            "fakulte", fakulteIstatistik,
            "bolum", bolumIstatistik,
            "sinif", sinifIstatistik
        ));
        response.put("filtreler", Map.of(
            "danismanAd", danismanAd != null ? danismanAd : "",
            "danismanSoyad", danismanSoyad != null ? danismanSoyad : "",
            "danismanAdSoyad", danismanAdSoyad != null ? danismanAdSoyad : ""
        ));
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("message", filteredOgrenciler.size() > 0 ? 
            "Danışman öğrencileri başarıyla getirildi" : "Belirtilen danışmana ait öğrenci bulunamadı");
        
        return ResponseEntity.ok(response);
    }

    // Validation helper method
    private void validateNotEmpty(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(
                String.format("%s cannot be null or empty", fieldName), 
                fieldName, 
                value
            );
        }
    }
}