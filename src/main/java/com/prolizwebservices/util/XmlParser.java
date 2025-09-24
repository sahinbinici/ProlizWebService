package com.prolizwebservices.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.prolizwebservices.model.Ders;
import com.prolizwebservices.model.Ogrenci;
import com.prolizwebservices.model.OgretimElemani;

/**
 * SOAP XML response'larını parse eden utility sınıfı
 */
@Component
public class XmlParser {

    private static final Logger logger = LoggerFactory.getLogger(XmlParser.class);

    /**
     * UzaktanEgitimDersleri SOAP response'unu parse eder
     */
    public List<Ders> parseDersler(String xmlResponse) {
        List<Ders> dersler = new ArrayList<>();
        
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            logger.warn("XML response boş");
            return dersler;
        }
        
        // DEBUG: SOAP response'u görmek için
        logger.info("🔍 SOAP Response uzunluğu: {} karakter", xmlResponse.length());
        if (xmlResponse.length() < 1000) {
            logger.info("🔍 SOAP Response: {}", xmlResponse);
        } else {
            logger.info("🔍 SOAP Response preview: {}", xmlResponse.substring(0, 500) + "...");
        }

        try {
            // <UzaktanEgitimDersleri> elementlerini bul
            Pattern dersPattern = Pattern.compile("<UzaktanEgitimDersleri>(.*?)</UzaktanEgitimDersleri>", 
                Pattern.DOTALL);
            Matcher dersMatcher = dersPattern.matcher(xmlResponse);

            while (dersMatcher.find()) {
                String dersXml = dersMatcher.group(1);
                Ders ders = new Ders();

                ders.setDersHarId(extractValue(dersXml, "DERS_HAR_ID"));
                ders.setDonemAd(extractValue(dersXml, "DONEM_AD"));
                ders.setDonemId(extractValue(dersXml, "DONEM_ID"));
                ders.setDersKodu(extractValue(dersXml, "DERS_KODU"));
                ders.setDersAdi(extractValue(dersXml, "DERS_ADI"));
                ders.setDersAdiEn(extractValue(dersXml, "DERS_ADI_EN"));
                ders.setDersYukuTeorik(extractValue(dersXml, "DERS_YUKU_TEORIK"));
                ders.setDersYukuUygulama(extractValue(dersXml, "DERS_YUKU_UYGULAMA"));
                ders.setFakAd(extractValue(dersXml, "FAK_AD"));
                ders.setBolAd(extractValue(dersXml, "BOL_AD"));
                ders.setProgAd(extractValue(dersXml, "PROG_AD"));
                ders.setDersSubeKod(extractValue(dersXml, "DERS_SUBE_KOD"));
                ders.setAkts(extractValue(dersXml, "AKTS"));
                ders.setKredi(extractValue(dersXml, "KREDI"));
                ders.setSinif(extractValue(dersXml, "SINIF"));
                ders.setOgretimElemani(extractValue(dersXml, "OGRETIM_ELEMANI"));
                ders.setOgretimElemaniTC(extractValue(dersXml, "OGRETIM_ELEMANI_TC"));

                if (ders.getDersHarId() != null && !ders.getDersHarId().isEmpty()) {
                    dersler.add(ders);
                }
            }

            logger.info("Toplam {} ders parse edildi", dersler.size());
            
        } catch (Exception e) {
            logger.error("Ders parsing hatası: {}", e.getMessage(), e);
        }

        return dersler;
    }

    /**
     * DersiVerenOgretimElamaniGetir SOAP response'unu parse eder
     */
    public List<OgretimElemani> parseOgretimElemanlari(String xmlResponse) {
        List<OgretimElemani> ogretimElemanlari = new ArrayList<>();
        
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            return ogretimElemanlari;
        }

        try {
            // <AkademikPersonel> elementlerini bul
            Pattern elemanPattern = Pattern.compile("<AkademikPersonel>(.*?)</AkademikPersonel>", 
                Pattern.DOTALL);
            Matcher elemanMatcher = elemanPattern.matcher(xmlResponse);

            while (elemanMatcher.find()) {
                String elemanXml = elemanMatcher.group(1);
                OgretimElemani eleman = new OgretimElemani();

                eleman.setTcKimlikNo(extractValue(elemanXml, "TC_KIMLIK_NO"));
                eleman.setSicilNo(extractValue(elemanXml, "SICIL_NO"));
                eleman.setAdi(extractValue(elemanXml, "ADI"));
                eleman.setSoyadi(extractValue(elemanXml, "SOYADI"));
                eleman.setUnvan(extractValue(elemanXml, "UNVAN"));
                eleman.setPersonelTip(extractValue(elemanXml, "PERSONEL_TIP"));
                eleman.setFakAd(extractValue(elemanXml, "FAK_AD"));
                eleman.setBolAd(extractValue(elemanXml, "BOL_AD"));
                eleman.setProgAd(extractValue(elemanXml, "PROG_AD"));
                eleman.setePosta(extractValue(elemanXml, "E_POSTA"));

                if (eleman.getTcKimlikNo() != null && !eleman.getTcKimlikNo().isEmpty()) {
                    ogretimElemanlari.add(eleman);
                }
            }

            logger.info("Toplam {} öğretim elemanı parse edildi", ogretimElemanlari.size());
            
        } catch (Exception e) {
            logger.error("Öğretim elemanı parsing hatası: {}", e.getMessage(), e);
        }

        return ogretimElemanlari;
    }

    /**
     * UzaktanEgitimDersiAlanOgrencileri SOAP response'unu parse eder
     */
    public List<Ogrenci> parseOgrenciler(String xmlResponse, String dersHarId) {
        List<Ogrenci> ogrenciler = new ArrayList<>();
        
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            return ogrenciler;
        }

        try {
            // <UzaktanEgitimDersiAlanOgrenciler> elementlerini bul
            Pattern ogrenciPattern = Pattern.compile("<UzaktanEgitimDersiAlanOgrenciler>(.*?)</UzaktanEgitimDersiAlanOgrenciler>", 
                Pattern.DOTALL);
            Matcher ogrenciMatcher = ogrenciPattern.matcher(xmlResponse);

            while (ogrenciMatcher.find()) {
                String ogrenciXml = ogrenciMatcher.group(1);
                Ogrenci ogrenci = new Ogrenci();

                ogrenci.setOgrNo(extractValue(ogrenciXml, "OGR_NO"));
                ogrenci.setTcKimlikNo(extractValue(ogrenciXml, "TCKIMLIKNO"));
                ogrenci.setAdi(extractValue(ogrenciXml, "ADI"));
                ogrenci.setSoyadi(extractValue(ogrenciXml, "SOYADI"));
                ogrenci.setFakulte(extractValue(ogrenciXml, "FAKULTE"));
                ogrenci.setBolum(extractValue(ogrenciXml, "BOLUM"));
                ogrenci.setProgram(extractValue(ogrenciXml, "PROGRAM"));
                ogrenci.setSinif(extractValue(ogrenciXml, "SINIF"));
                ogrenci.setKayitNeden(extractValue(ogrenciXml, "KAYIT_NEDEN"));
                ogrenci.setOgrenimDurum(extractValue(ogrenciXml, "OGRENIM_DURUM"));
                ogrenci.setDersKredi(extractValue(ogrenciXml, "DERS_KREDI"));
                ogrenci.setDersAkts(extractValue(ogrenciXml, "DERS_AKTS"));
                ogrenci.setDersHarId(dersHarId); // Hangi derse ait olduğunu set et

                if (ogrenci.getOgrNo() != null && !ogrenci.getOgrNo().isEmpty()) {
                    ogrenciler.add(ogrenci);
                }
            }

            logger.info("Toplam {} öğrenci parse edildi (Ders: {})", ogrenciler.size(), dersHarId);
            
        } catch (Exception e) {
            logger.error("Öğrenci parsing hatası: {}", e.getMessage(), e);
        }

        return ogrenciler;
    }

    /**
     * XML'den belirtilen tag'in değerini çıkarır
     */
    private String extractValue(String xml, String tagName) {
        try {
            Pattern pattern = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(xml);
            
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                return value.isEmpty() ? null : value;
            }
        } catch (Exception e) {
            logger.debug("Tag '{}' çıkarılırken hata: {}", tagName, e.getMessage());
        }
        
        return null;
    }
}
