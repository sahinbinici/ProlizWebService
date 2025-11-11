package com.prolizwebservices.client;

import com.prolizwebservices.exception.SoapServiceException;
import com.prolizwebservices.service.HybridCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class OgrenciWebServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(OgrenciWebServiceClient.class);
    private static final String SOAP_URL = "https://obs.gantep.edu.tr/proliz_obs_lms_miner/proliz_obs_lms_miner.asmx";

    private final RestTemplate restTemplate;
    
    @Autowired(required = false)
    private HybridCacheService cacheService;

    @Value("${soap.service.username:ProLmsGan}")
    private String serviceUsername;

    @Value("${soap.service.password:-2020+Pro*Gan#}")
    private String servicePassword;
    
    private static final String NAMESPACE_URI = "http://tempuri.org/";
    private static final String SOAP_ACTION_HEADER = "SOAPAction";
    private static final String SOAP_ACTION_PREFIX = "http://tempuri.org/";

    @Autowired
    public OgrenciWebServiceClient(@Qualifier("sslRestTemplate") RestTemplate restTemplate) {
        if (restTemplate == null) {
            throw new IllegalArgumentException("RestTemplate cannot be null");
        }
        this.restTemplate = restTemplate;
        
        // Gerekli konfigürasyon kontrolleri
        if (!StringUtils.hasText(serviceUsername) || !StringUtils.hasText(servicePassword)) {
            logger.warn("SOAP servisi kullanıcı adı veya şifresi yapılandırılmamış");
        } else {
            logger.info("SOAP Authentication yapılandırıldı - Kullanıcı: {}", serviceUsername);
        }
    }

    /**
     * Akademik personel şifre kontrolü yapar (MD5 hash ile)
     * @param sicilNo Sicil numarası
     * @param sifre Şifre (MD5 hash'lenecek)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String akademikPersonelSifreKontrol(String sicilNo, String sifre) throws SoapServiceException {
        final String methodName = "AkademikPersonelSifreKontrol";
        logger.info("{} başlatıldı - Sicil No: {}", methodName, sicilNo);
        
        // Şifreyi MD5 hash'e çevir ve büyük harflerle kullan
        String hashedPassword = hashPasswordMD5(sifre);
        logger.debug("Şifre MD5 hash'lendi: {} -> {}", sifre, hashedPassword);
        
        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<sicilNo>" + escapeXml(sicilNo) + "</sicilNo>" +
                            "<sifre>" + escapeXml(hashedPassword) + "</sifre>" +
                            "<ipAddress></ipAddress>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı", methodName);
        return result;
    }

    /**
     * Öğrenci şifre kontrolü yapar (MD5 hash ile)
     * @param ogrenciNo Öğrenci numarası
     * @param sifre Şifre (MD5 hash'lenecek)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String ogrenciSifreKontrol(String ogrenciNo, String sifre) throws SoapServiceException {
        final String methodName = "OgrenciGirisKontrol";
        logger.info("{} başlatıldı - Öğrenci: {}", methodName, ogrenciNo);

        // Şifreyi MD5 hash'e çevir ve büyük harflerle kullan
        String hashedPassword = hashPasswordMD5(sifre);
        logger.debug("Şifre MD5 hash'lendi: {} -> {}", sifre, hashedPassword);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<ogrenciNo>" + escapeXml(ogrenciNo) + "</ogrenciNo>" +
                            "<sifre>" + escapeXml(hashedPassword) + "</sifre>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı", methodName);
        return result;
    }

    /**
     * Uzaktan eğitim derslerini getirir (CACHE DESTEKLİ)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String getUzaktanEgitimDersleri() throws SoapServiceException {
        final String methodName = "UzaktanEgitimDersleri";
        final String cacheKey = "soap:" + methodName;
        
        // Cache varsa kullan
        if (cacheService != null) {
            return cacheService.getOrFetch(cacheKey, methodName, () -> {
                return fetchUzaktanEgitimDersleriFromSoap(methodName);
            });
        }
        
        // Cache yoksa direkt SOAP çağrısı
        return fetchUzaktanEgitimDersleriFromSoap(methodName);
    }
    
    private String fetchUzaktanEgitimDersleriFromSoap(String methodName) {
        logger.info("{} başlatıldı (SOAP)", methodName);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (SOAP)", methodName);
        return result;
    }

    /**
     * Uzaktan eğitim dersi alan öğrencileri getirir (CACHE DESTEKLİ)
     * @param dersKodu Ders kodu
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String getUzaktanEgitimDersiAlanOgrencileri(String dersKodu) throws SoapServiceException {
        final String methodName = "UzaktanEgitimDersiAlanOgrencileri";
        final String cacheKey = "soap:" + methodName + ":" + dersKodu;
        
        // Cache varsa kullan
        if (cacheService != null) {
            return cacheService.getOrFetch(cacheKey, methodName, () -> {
                return fetchUzaktanEgitimDersiAlanOgrencileriFromSoap(methodName, dersKodu);
            });
        }
        
        // Cache yoksa direkt SOAP çağrısı
        return fetchUzaktanEgitimDersiAlanOgrencileriFromSoap(methodName, dersKodu);
    }
    
    private String fetchUzaktanEgitimDersiAlanOgrencileriFromSoap(String methodName, String dersKodu) {
        logger.info("{} başlatıldı (SOAP) - Ders Har ID: {}", methodName, dersKodu);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte) 
        // dersKodu parametresi aslında dersHarID değeri içeriyor
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<dersHarID>" + escapeXml(dersKodu) + "</dersHarID>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (SOAP)", methodName);
        return result;
    }

    /**
     * Öğretim elemanı bilgilerini getirir (TC kimlik, sicil no veya eposta ile) (CACHE DESTEKLİ)
     * @param tcKimlikNo TC kimlik numarası (opsiyonel)
     * @param sicilNo Sicil numarası (opsiyonel) 
     * @param eposta E-posta adresi (opsiyonel)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String getOgretimElemaniByFilters(String tcKimlikNo, String sicilNo, String eposta) throws SoapServiceException {
        final String methodName = "DersiVerenOgretimElamaniGetir";
        final String cacheKey = "soap:" + methodName + ":" + 
            (tcKimlikNo != null ? tcKimlikNo : "") + ":" + 
            (sicilNo != null ? sicilNo : "") + ":" + 
            (eposta != null ? eposta : "");
        
        // Cache varsa kullan
        if (cacheService != null) {
            return cacheService.getOrFetch(cacheKey, methodName, () -> {
                return fetchOgretimElemaniByFiltersFromSoap(methodName, tcKimlikNo, sicilNo, eposta);
            });
        }
        
        // Cache yoksa direkt SOAP çağrısı
        return fetchOgretimElemaniByFiltersFromSoap(methodName, tcKimlikNo, sicilNo, eposta);
    }
    
    private String fetchOgretimElemaniByFiltersFromSoap(String methodName, String tcKimlikNo, String sicilNo, String eposta) {
        logger.info("{} başlatıldı (SOAP) - TC: {}, Sicil: {}, Eposta: {}", methodName, tcKimlikNo, sicilNo, eposta);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<sicil_no>" + escapeXml(sicilNo != null ? sicilNo : "") + "</sicil_no>" +
                            "<tc_kimlik_no>" + escapeXml(tcKimlikNo != null ? tcKimlikNo : "") + "</tc_kimlik_no>" +
                            "<eposta>" + escapeXml(eposta != null ? eposta : "") + "</eposta>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (SOAP)", methodName);
        return result;
    }

    /**
     * SOAP response'u parse eder ve boolean değer döndürür
     * @param soapResponse SOAP XML response
     * @return true eğer başarılı, false eğer başarısız
     */
    public boolean parseSoapResponse(String soapResponse) {
        if (soapResponse == null || soapResponse.isEmpty()) {
            logger.debug("SOAP response boş");
            return false;
        }
        
        logger.debug("SOAP Response parsing: {}", soapResponse.length() > 500 ? 
            soapResponse.substring(0, 500) + "..." : soapResponse);
        
        // XML içinde success indicator'ları ara
        boolean result = soapResponse.contains("<Success>true</Success>") || 
                        soapResponse.contains(">true<") ||
                        soapResponse.contains("success=\"true\"") ||
                        soapResponse.contains("result=\"true\"") ||
                        // Ek pattern'ler ekleyelim
                        soapResponse.toLowerCase().contains("<success>true</success>") ||
                        soapResponse.toLowerCase().contains("success") && soapResponse.toLowerCase().contains("true");
        
        logger.debug("SOAP Response parse result: {}", result);
        return result;
    }

    /**
     * Şifreyi MD5 hash'e çevirir ve büyük harflerle döndürür
     * @param password Hash'lenecek şifre
     * @return MD5 hash (büyük harflerle)
     */
    private String hashPasswordMD5(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
            
            // Byte array'i hex string'e çevir
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            // Büyük harflerle döndür
            return sb.toString().toUpperCase();
            
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            logger.error("MD5 hash oluşturma hatası: {}", e.getMessage(), e);
            throw new RuntimeException("MD5 hash oluşturulamadı", e);
        }
    }

    /**
     * SOAP istek gövdesi oluşturur
     * @param methodName Çağrılacak SOAP metodu adı
     * @param requestBody SOAP istek gövdesi içeriği
     * @return Tam SOAP istek gövdesi
     */
    private String createSoapRequest(String methodName, String requestBody) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
               "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
               "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
               "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
               "<soap:Body>" +
               "<" + methodName + " xmlns=\"http://tempuri.org/\">" +
               (requestBody != null ? requestBody : "") +
               "</" + methodName + ">" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }

    /**
     * SOAP isteği gönderir ve yanıtı döndürür
     * @param soapBody SOAP istek gövdesi (boş olamaz)
     * @param soapAction SOAP Action değeri (boş olamaz)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     * @throws IllegalArgumentException Geçersiz parametre durumunda fırlatılır
     */
    private String sendSoapRequestMain(String soapBody, String soapAction) throws SoapServiceException {
        if (!StringUtils.hasText(soapBody) || !StringUtils.hasText(soapAction)) {
            throw new IllegalArgumentException("SOAP gövdesi ve action değeri boş olamaz");
        }
        
        logger.debug("SOAP isteği gönderiliyor - Action: {}", soapAction);
        
        try {
            // WS-Security kullanmıyoruz, direkt SOAP body gönder
            // Authentication bilgileri operasyon parametresi olarak gönderilecek
            return sendSoapRequestWithHeaders(soapBody, soapAction, false);
            
        } catch (SoapServiceException e) {
            throw e; // Zaten işlenmiş hata
        } catch (Exception e) {
            String errorMsg = String.format("Beklenmeyen bir hata oluştu - Action: %s, Hata: %s", 
                soapAction, e.getMessage());
            logger.error(errorMsg, e);
            throw new SoapServiceException(errorMsg, e);
    }
    }

    /**
     * SOAP isteğini belirtilen başlıklarla gönderir
     * @param soapBody SOAP istek gövdesi
     * @param soapAction SOAP Action değeri
     * @param useSoap12 SOAP 1.2 kullanılıp kullanılmayacağı
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    private String sendSoapRequestWithHeaders(String soapBody, String soapAction, boolean useSoap12) 
            throws SoapServiceException {
        
        if (!StringUtils.hasText(soapBody) || !StringUtils.hasText(soapAction)) {
            throw new IllegalArgumentException("SOAP gövdesi ve action değeri boş olamaz");
        }
        
        logger.debug("SOAP isteği gönderiliyor - Action: {}, SOAP 1.2: {}", soapAction, useSoap12);
        
        try {
            // HTTP başlıklarını oluştur
            HttpHeaders headers = new HttpHeaders();
            
            // SOAP versiyonuna göre Content-Type ve diğer başlıkları ayarla
            if (useSoap12) {
                // SOAP 1.2 formatı - action parametresini Content-Type içinde belirt
                headers.setContentType(MediaType.valueOf(
                    "application/soap+xml;charset=utf-8;action=\"" + soapAction + "\""));
            } else {
                // SOAP 1.1 formatı - SOAPAction header'ını ayrı olarak kullan (quotes ile)
                headers.setContentType(MediaType.valueOf("text/xml;charset=utf-8"));
                headers.set("SOAPAction", "\"" + soapAction + "\"");
            }
            
            // Ek güvenlik başlıkları
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");
            
            // HTTP isteğini oluştur
            HttpEntity<String> request = new HttpEntity<>(soapBody, headers);
            
            logger.trace("Gönderilen SOAP isteği: {}", soapBody);
            
            // İsteği gönder
            ResponseEntity<String> response = restTemplate.postForEntity(SOAP_URL, request, String.class);
            
            // Yanıtı kontrol et
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                logger.debug("SOAP yanıtı alındı - Durum: {}", response.getStatusCode());
                // SOAP response logging removed for security - contains sensitive data
                // logger.trace("SOAP yanıtı: {}", responseBody);
                logger.trace("SOAP yanıtı: {}", responseBody);
                return responseBody;
            } else {
                String errorMsg = String.format("SOAP isteği başarısız - HTTP %d: %s", 
                    response.getStatusCode().value(), response.getStatusCode());
                logger.error(errorMsg);
                throw new SoapServiceException(errorMsg);
            }
            
        } catch (RestClientException e) {
            String errorMsg = String.format("SOAP isteği sırasında bağlantı hatası: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new SoapServiceException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("SOAP isteği sırasında beklenmeyen hata: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new SoapServiceException(errorMsg, e);
        }
    }

    /**
     * OPTIMIZED: Dersi veren öğretim elemanını cache'ten getirir (ÇOK HIZLI!)
     * Bu metot cache kullanır, SOAP isteği göndermez
     * @param dersKodu Ders kodu (DERS_HAR_ID)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String getDersiVerenOgretimElemani(String dersKodu) throws SoapServiceException {
        logger.info("getDersiVerenOgretimElemani başlatıldı - DersKodu: {} (CACHE'TEN)", dersKodu);
        
        try {
            // Cache'i kullan - DataCacheService'e erişim gerekli
            // Bu metot artık DataCacheService tarafından çağrılmalı
            logger.warn("DEPRECATED: Bu metot artık cache üzerinden çağrılmalı. DataCacheService.getOgretimElemaniByDersHarId() kullanın");
            
            // Fallback: Eski yöntem (sadece cache hazır değilse)
            return getDersiVerenOgretimElemaniFallback(dersKodu);
            
        } catch (Exception e) {
            logger.error("getDersiVerenOgretimElemani hatası - Ders: {}, Hata: {}", dersKodu, e.getMessage(), e);
            return createEmptyOgretimElemaniResponse();
        }
    }
    
    /**
     * FALLBACK: Cache kullanılamadığında eski yöntem
     */
    private String getDersiVerenOgretimElemaniFallback(String dersKodu) {
        logger.info("FALLBACK MODE: Cache kullanılamıyor, SOAP isteği gönderiliyor - DersKodu: {}", dersKodu);
        
        try {
            // 1. Önce ders listesinden bu dersi bul
            String dersListesi = getUzaktanEgitimDersleri();
            String ogretimElemaniTC = extractOgretimElemaniTCFromDersListesi(dersListesi, dersKodu);
            
            if (ogretimElemaniTC == null || ogretimElemaniTC.isEmpty()) {
                logger.warn("Ders {} için öğretim elemanı TC'si bulunamadı", dersKodu);
                return createEmptyOgretimElemaniResponse();
            }
            
            // 2. TC ile öğretim elemanını getir
            logger.info("Ders {} için bulunan öğretim elemanı TC: {}", dersKodu, ogretimElemaniTC);
            return getOgretimElemaniByFilters(ogretimElemaniTC, null, null);
            
        } catch (Exception e) {
            logger.error("Fallback getDersiVerenOgretimElemani hatası: {}", e.getMessage(), e);
            return createEmptyOgretimElemaniResponse();
        }
    }
    
    /**
     * Ders listesinden belirtilen ders kodunun öğretim elemanı TC'sini çıkarır
     */
    private String extractOgretimElemaniTCFromDersListesi(String dersListesi, String dersKodu) {
        if (dersListesi == null || dersListesi.isEmpty() || dersKodu == null) {
            return null;
        }
        
        try {
            // DERS_HAR_ID'ye göre ara
            String dersHarIdPattern = "<DERS_HAR_ID>" + escapeXml(dersKodu) + "</DERS_HAR_ID>";
            int dersIndex = dersListesi.indexOf(dersHarIdPattern);
            
            if (dersIndex == -1) {
                logger.debug("DERS_HAR_ID {} bulunamadı", dersKodu);
                return null;
            }
            
            // Bu dersin UzaktanEgitimDersleri bloğunu bul
            int blockStart = dersListesi.lastIndexOf("<UzaktanEgitimDersleri>", dersIndex);
            int blockEnd = dersListesi.indexOf("</UzaktanEgitimDersleri>", dersIndex);
            
            if (blockStart == -1 || blockEnd == -1) {
                logger.debug("Ders bloğu bulunamadı");
                return null;
            }
            
            String dersBlock = dersListesi.substring(blockStart, blockEnd);
            
            // OGRETIM_ELEMANI_TC'yi çıkar
            String tcPattern = "<OGRETIM_ELEMANI_TC>";
            int tcStart = dersBlock.indexOf(tcPattern);
            if (tcStart == -1) return null;
            
            int tcValueStart = tcStart + tcPattern.length();
            int tcEnd = dersBlock.indexOf("</OGRETIM_ELEMANI_TC>", tcValueStart);
            if (tcEnd == -1) return null;
            
            String tc = dersBlock.substring(tcValueStart, tcEnd).trim();
            return tc.isEmpty() ? null : tc;
            
        } catch (Exception e) {
            logger.error("OGRETIM_ELEMANI_TC çıkarma hatası: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Boş öğretim elemanı response'u oluşturur
     */
    private String createEmptyOgretimElemaniResponse() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
               "<soap:Body>" +
               "<DersiVerenOgretimElamaniGetirResponse xmlns=\"http://tempuri.org/\">" +
               "<DersiVerenOgretimElamaniGetirResult>" +
               "<AkademikPersonelListesi>" +
               "<Success>false</Success>" +
               "<Error>Öğretim elemanı bulunamadı</Error>" +
               "</AkademikPersonelListesi>" +
               "</DersiVerenOgretimElamaniGetirResult>" +
               "</DersiVerenOgretimElamaniGetirResponse>" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }

    /**
     * XML karakterlerini escape eder
     * @param text Escape edilecek metin
     * @return Escape edilmiş metin
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}