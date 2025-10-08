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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prolizwebservices.client.OgrenciWebServiceClient;
import com.prolizwebservices.exception.ValidationException;
import com.prolizwebservices.model.Ders;
import com.prolizwebservices.model.Ogrenci;
import com.prolizwebservices.model.OgretimElemani;
import com.prolizwebservices.util.XmlParser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"*"}, 
            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
            allowCredentials = "false")
@Tag(name = "ProlizWebServices", description = "SOAP to REST adapter for Gaziantep University Student Information System")
public class ProlizWebServiceController {

    private final OgrenciWebServiceClient webServiceClient;
    private final XmlParser xmlParser;

    @Autowired
    public ProlizWebServiceController(OgrenciWebServiceClient webServiceClient, XmlParser xmlParser) {
        this.webServiceClient = webServiceClient;
        this.xmlParser = xmlParser;
    }
    
    // 1. Academic Staff Authentication
    @Operation(
        summary = "Academic Staff Password Verification",
        description = "Authenticates academic staff members using their username and password credentials. Password is automatically hashed with MD5."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication request processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "503", description = "Remote SOAP service unavailable")
    })
    @PostMapping("/akademik-personel/sifre-kontrol")
    public ResponseEntity<String> akademikPersonelSifreKontrol(
            @Parameter(description = "Academic staff username (Sicil No)", required = true, example = "78555023")
            @RequestParam String kullaniciAdi,
            @Parameter(description = "Academic staff password (will be MD5 hashed)", required = true, example = "password123")
            @RequestParam String sifre) {
        
        validateNotEmpty(kullaniciAdi, "kullaniciAdi");
        validateNotEmpty(sifre, "sifre");
        
        String result = webServiceClient.akademikPersonelSifreKontrol(kullaniciAdi, sifre);
        return ResponseEntity.ok(result);
    }

    // 2. Student Authentication
    @Operation(
        summary = "Student Password Verification",
        description = "Authenticates students using their student number and password credentials."
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
            @Parameter(description = "Student password", required = true, example = "password123")
            @RequestParam String sifre) {
        
        validateNotEmpty(ogrenciNo, "ogrenciNo");
        validateNotEmpty(sifre, "sifre");
        
        String result = webServiceClient.ogrenciSifreKontrol(ogrenciNo, sifre);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.contains("<Success>true</Success>") || result.contains(">true<"));
        response.put("message", response.get("success").equals(true) ? 
            "Öğrenci girişi başarılı" : "Kullanıcı adı veya şifre hatalı");
        response.put("studentNumber", ogrenciNo);
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    // 3. Distance Education Courses
    @Operation(
        summary = "Get Distance Education Courses",
        description = "Retrieves a list of all available distance education courses."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Course list retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "Remote SOAP service unavailable")
    })
    @GetMapping("/uzaktan-egitim/dersler")
    public ResponseEntity<Map<String, Object>> getUzaktanEgitimDersleri(
            @Parameter(description = "Maximum number of courses to return (default: 50)", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        
        String xmlResult = webServiceClient.getUzaktanEgitimDersleri();
        List<Ders> allDersler = xmlParser.parseDersler(xmlResult);
        
        // Limit uygula (Swagger UI için)
        List<Ders> limitedDersler = allDersler.stream()
            .limit(Math.min(limit, 100)) // Max 100 ile sınırla
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("dersler", limitedDersler);
        response.put("toplamDers", allDersler.size());
        response.put("gosterilenDers", limitedDersler.size());
        response.put("message", "Swagger UI performansı için " + limitedDersler.size() + " ders gösteriliyor");
        
        return ResponseEntity.ok(response);
    }

    // 4. Students in Distance Education Course
    @Operation(
        summary = "Get Students Enrolled in Distance Education Course",
        description = "Retrieves students enrolled in a specific distance education course."
    )
    @GetMapping("/uzaktan-egitim/ders/{dersHarID}/ogrenciler")
    public ResponseEntity<List<Ogrenci>> getUzaktanEgitimDersiAlanOgrenciler(
            @Parameter(description = "Ders_Hard_ID", required = true, example = "BIL101")
            @PathVariable String dersHarID) {
        validateNotEmpty(dersHarID, "dersHarID");
        
        String xmlResult = webServiceClient.getUzaktanEgitimDersiAlanOgrencileri(dersHarID);
        List<Ogrenci> ogrenciler = xmlParser.parseOgrenciler(xmlResult, dersHarID);
        return ResponseEntity.ok(ogrenciler);
    }

    // 5. Course Instructor
    @Operation(
        summary = "Get Course Instructor",
        description = "Retrieves the instructor information for a specific course."
    )
    @GetMapping("/ders/{dersHarID}/ogretim-elemani")
    public ResponseEntity<List<OgretimElemani>> getDersiVerenOgretimElemani(
            @Parameter(description = "Ders_Hard_ID", required = true, example = "2838793")
            @PathVariable String dersHarID) {
        validateNotEmpty(dersHarID, "dersHarID");
        
        // ⚠️ DEPRECATED: Artık DataController kullanın - bu endpoint cache kullanmıyor!
        // Geçici olarak eski yöntemi kullan
        String xmlResult = webServiceClient.getDersiVerenOgretimElemani(dersHarID);
        List<OgretimElemani> ogretimElemanlari = xmlParser.parseOgretimElemanlari(xmlResult);
        return ResponseEntity.ok(ogretimElemanlari);
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
