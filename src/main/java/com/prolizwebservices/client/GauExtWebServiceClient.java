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

@Component
public class GauExtWebServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(GauExtWebServiceClient.class);
    private static final String SOAP_URL = "https://obs.gantep.edu.tr/proliz_gau_ext_miner/proliz_gau_ext_miner.asmx";

    private final RestTemplate restTemplate;
    
    @Autowired(required = false)
    private HybridCacheService cacheService;

    @Value("${soap.gau.ext.username:ProGauExtBim}")
    private String serviceUsername;

    @Value("${soap.gau.ext.password:#Pro_Gau!2019+}")
    private String servicePassword;
    
    private static final String NAMESPACE_URI = "http://tempuri.org/";
    private static final String SOAP_ACTION_HEADER = "SOAPAction";
    private static final String SOAP_ACTION_PREFIX = "http://tempuri.org/";

    @Autowired
    public GauExtWebServiceClient(@Qualifier("sslRestTemplate") RestTemplate restTemplate) {
        if (restTemplate == null) {
            throw new IllegalArgumentException("RestTemplate cannot be null");
        }
        this.restTemplate = restTemplate;
        
        // Gerekli konfigürasyon kontrolleri
        if (!StringUtils.hasText(serviceUsername) || !StringUtils.hasText(servicePassword)) {
            logger.warn("GAU EXT SOAP servisi kullanıcı adı veya şifresi yapılandırılmamış");
        } else {
            logger.info("GAU EXT SOAP Authentication yapılandırıldı - Kullanıcı: {}", serviceUsername);
        }
    }

    /**
     * Öğrenci istatistiklerini getirir (CACHE DESTEKLİ)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String getOgrenciIstatistik() throws SoapServiceException {
        final String methodName = "OgrenciIstatistik";
        final String cacheKey = "soap:gau-ext:" + methodName;
        
        // Cache varsa kullan
        if (cacheService != null) {
            return cacheService.getOrFetch(cacheKey, methodName, () -> {
                return fetchOgrenciIstatistikFromSoap(methodName);
            });
        }
        
        // Cache yoksa direkt SOAP çağrısı
        return fetchOgrenciIstatistikFromSoap(methodName);
    }
    
    private String fetchOgrenciIstatistikFromSoap(String methodName) {
        logger.info("{} başlatıldı (GAU EXT SOAP)", methodName);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (GAU EXT SOAP)", methodName);
        return result;
    }

    /**
     * Aktif öğrenci listesini getirir (CACHE DESTEKLİ)
     * @param ogrNo Öğrenci numarası (opsiyonel)
     * @param tck TC kimlik numarası (opsiyonel)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String getAktifOgrenciListesi(String ogrNo, String tck) throws SoapServiceException {
        final String methodName = "AktifOgrenciListesiGetir";
        final String cacheKey = "soap:gau-ext:" + methodName + ":" + 
            (ogrNo != null ? ogrNo : "") + ":" + 
            (tck != null ? tck : "");
        
        // Cache varsa kullan
        if (cacheService != null) {
            return cacheService.getOrFetch(cacheKey, methodName, () -> {
                return fetchAktifOgrenciListesiFromSoap(methodName, ogrNo, tck);
            });
        }
        
        // Cache yoksa direkt SOAP çağrısı
        return fetchAktifOgrenciListesiFromSoap(methodName, ogrNo, tck);
    }
    
    private String fetchAktifOgrenciListesiFromSoap(String methodName, String ogrNo, String tck) {
        logger.info("{} başlatıldı (GAU EXT SOAP) - Öğr No: {}, TCK: {}", methodName, ogrNo, tck);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<ogr_no>" + escapeXml(ogrNo != null ? ogrNo : "") + "</ogr_no>" +
                            "<tck>" + escapeXml(tck != null ? tck : "") + "</tck>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (GAU EXT SOAP)", methodName);
        return result;
    }

    /**
     * Akademik personel şifre kontrolü yapar (MD5 hash ile)
     * @param sicilNo Sicil numarası
     * @param sifre Şifre (MD5 hash'lenecek)
     * @param ipAddress IP adresi (opsiyonel)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String akademikPersonelSifreKontrol(String sicilNo, String sifre, String ipAddress) throws SoapServiceException {
        final String methodName = "AkademikPersonelSifreKontrol";
        logger.info("{} başlatıldı (GAU EXT) - Sicil No: {}", methodName, sicilNo);
        
        // Şifreyi MD5 hash'e çevir ve büyük harflerle kullan
        String hashedPassword = hashPasswordMD5(sifre);
        logger.debug("Şifre MD5 hash'lendi: {} -> {}", sifre, hashedPassword);
        
        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<sicilNo>" + escapeXml(sicilNo) + "</sicilNo>" +
                            "<sifre>" + escapeXml(hashedPassword) + "</sifre>" +
                            "<ipAddress>" + escapeXml(ipAddress != null ? ipAddress : "") + "</ipAddress>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (GAU EXT)", methodName);
        return result;
    }

    /**
     * Öğrenci giriş kontrolü yapar (MD5 hash ile)
     * @param ogrenciNo Öğrenci numarası
     * @param sifre Şifre (MD5 hash'lenecek)
     * @param ipAddress IP adresi (opsiyonel)
     * @return SOAP yanıtı
     * @throws SoapServiceException SOAP hatası durumunda fırlatılır
     */
    public String ogrenciGirisKontrol(String ogrenciNo, String sifre, String ipAddress) throws SoapServiceException {
        final String methodName = "OgrenciGirisKontrol";
        logger.info("{} başlatıldı (GAU EXT) - Öğrenci: {}", methodName, ogrenciNo);

        // Şifreyi MD5 hash'e çevir ve büyük harflerle kullan
        String hashedPassword = hashPasswordMD5(sifre);
        logger.debug("Şifre MD5 hash'lendi: {} -> {}", sifre, hashedPassword);

        // SOAP istek gövdesini oluştur (authentication bilgileri ile birlikte)
        String requestBody = "<userName>" + escapeXml(serviceUsername) + "</userName>" +
                            "<password>" + escapeXml(servicePassword) + "</password>" +
                            "<ogrenciNo>" + escapeXml(ogrenciNo) + "</ogrenciNo>" +
                            "<sifre>" + escapeXml(hashedPassword) + "</sifre>" +
                            "<ipAddress>" + escapeXml(ipAddress != null ? ipAddress : "") + "</ipAddress>";

        // SOAP envelope oluştur
        String soapBody = createSoapRequest(methodName, requestBody);
        
        // SOAP isteğini gönder
        String result = sendSoapRequestMain(soapBody, "http://tempuri.org/" + methodName);
        logger.info("{} tamamlandı (GAU EXT)", methodName);
        return result;
    }

    /**
     * SOAP response'u parse eder ve boolean değer döndürür
     * @param soapResponse SOAP XML response
     * @return true eğer başarılı, false eğer başarısız
     */
    public boolean parseSoapResponse(String soapResponse) {
        if (soapResponse == null || soapResponse.isEmpty()) {
            return false;
        }
        
        // XML içinde success indicator'ları ara
        return soapResponse.contains("<Sucess>true</Sucess>") || 
               soapResponse.contains(">true<") ||
               soapResponse.contains("success=\"true\"") ||
               soapResponse.contains("result=\"true\"");
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
        
        logger.debug("GAU EXT SOAP isteği gönderiliyor - Action: {}", soapAction);
        
        try {
            return sendSoapRequestWithHeaders(soapBody, soapAction, false);
            
        } catch (SoapServiceException e) {
            throw e; // Zaten işlenmiş hata
        } catch (Exception e) {
            String errorMsg = String.format("GAU EXT beklenmeyen bir hata oluştu - Action: %s, Hata: %s", 
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
        
        logger.debug("GAU EXT SOAP isteği gönderiliyor - Action: {}, SOAP 1.2: {}", soapAction, useSoap12);
        
        try {
            // HTTP başlıklarını oluştur
            HttpHeaders headers = new HttpHeaders();
            
            // SOAP versiyonuna göre Content-Type ve diğer başlıkları ayarla
            if (useSoap12) {
                headers.setContentType(MediaType.valueOf(
                    "application/soap+xml;charset=utf-8;action=\"" + soapAction + "\""));
            } else {
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
            
            logger.trace("Gönderilen GAU EXT SOAP isteği: {}", soapBody);
            
            // İsteği gönder
            ResponseEntity<String> response = restTemplate.postForEntity(SOAP_URL, request, String.class);
            
            // Yanıtı kontrol et
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                logger.debug("GAU EXT SOAP yanıtı alındı - Durum: {}", response.getStatusCode());
                logger.trace("GAU EXT SOAP yanıtı: {}", responseBody);
                return responseBody;
            } else {
                String errorMsg = String.format("GAU EXT SOAP isteği başarısız - HTTP %d: %s", 
                    response.getStatusCode().value(), response.getStatusCode());
                logger.error(errorMsg);
                throw new SoapServiceException(errorMsg);
            }
            
        } catch (RestClientException e) {
            String errorMsg = String.format("GAU EXT SOAP isteği sırasında bağlantı hatası: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new SoapServiceException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("GAU EXT SOAP isteği sırasında beklenmeyen hata: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new SoapServiceException(errorMsg, e);
        }
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