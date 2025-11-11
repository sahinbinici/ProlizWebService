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
import com.prolizwebservices.model.OgrenciIstatistik;
import com.prolizwebservices.model.AktifOgrenci;

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
     * OgrenciIstatistik SOAP response'unu parse eder
     */
    public List<OgrenciIstatistik> parseOgrenciIstatistik(String xmlResponse) {
        List<OgrenciIstatistik> istatistikler = new ArrayList<>();
        
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            logger.warn("Öğrenci istatistik XML response boş");
            return istatistikler;
        }
        
        logger.info("🔍 Öğrenci İstatistik SOAP Response uzunluğu: {} karakter", xmlResponse.length());
        
        try {
            // <Istatistik> elementlerini bul
            Pattern istatistikPattern = Pattern.compile("<Istatistik>(.*?)</Istatistik>", 
                Pattern.DOTALL);
            Matcher istatistikMatcher = istatistikPattern.matcher(xmlResponse);

            while (istatistikMatcher.find()) {
                String istatistikXml = istatistikMatcher.group(1);
                OgrenciIstatistik istatistik = new OgrenciIstatistik();

                istatistik.setOgrenciSayisi(extractValue(istatistikXml, "OGR_SAY"));
                istatistik.setAkademikPersonelSayisi(extractValue(istatistikXml, "AKD_SAY"));
                istatistik.setIdariPersonelSayisi(extractValue(istatistikXml, "IDR_SAY"));
                istatistik.setFakulteSayisi(extractValue(istatistikXml, "FAK_SAY"));
                istatistik.setMyoSayisi(extractValue(istatistikXml, "MYO_SAY"));
                istatistik.setYuksekOkulSayisi(extractValue(istatistikXml, "YO_SAY"));
                istatistik.setEnstituSayisi(extractValue(istatistikXml, "ENS_SAY"));

                istatistikler.add(istatistik);
            }

            logger.info("Toplam {} istatistik parse edildi", istatistikler.size());
            
        } catch (Exception e) {
            logger.error("Öğrenci istatistik parsing hatası: {}", e.getMessage(), e);
        }

        return istatistikler;
    }

    /**
     * AktifOgrenciListesiGetir SOAP response'unu parse eder
     */
    public List<AktifOgrenci> parseAktifOgrenciler(String xmlResponse) {
        List<AktifOgrenci> ogrenciler = new ArrayList<>();
        
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            logger.warn("Aktif öğrenci XML response boş");
            return ogrenciler;
        }
        
        logger.info("🔍 Aktif Öğrenci SOAP Response uzunluğu: {} karakter", xmlResponse.length());
        
        try {
            // <Ogrenci> elementlerini bul
            Pattern ogrenciPattern = Pattern.compile("<Ogrenci>(.*?)</Ogrenci>", 
                Pattern.DOTALL);
            Matcher ogrenciMatcher = ogrenciPattern.matcher(xmlResponse);

            while (ogrenciMatcher.find()) {
                String ogrenciXml = ogrenciMatcher.group(1);
                AktifOgrenci ogrenci = new AktifOgrenci();

                ogrenci.setAktifMi(extractValue(ogrenciXml, "AKTIFMI"));
                ogrenci.setTcKimlikNo(extractValue(ogrenciXml, "TC_KIMLIK_NO"));
                ogrenci.setOgrenciNo(extractValue(ogrenciXml, "OGRENCI_NO"));
                ogrenci.setAd(extractValue(ogrenciXml, "AD"));
                ogrenci.setSoyad(extractValue(ogrenciXml, "SOYAD"));
                ogrenci.setUyruk(extractValue(ogrenciXml, "UYRUK"));
                ogrenci.setDogumYeri(extractValue(ogrenciXml, "DOGUM_YERI"));
                ogrenci.setDogumTarihi(extractValue(ogrenciXml, "DOGUM_TARIHI"));
                ogrenci.setCinsiyet(extractValue(ogrenciXml, "CINSIYET"));
                ogrenci.setEgitimDerecesi(extractValue(ogrenciXml, "EGITIM_DERECESI"));
                ogrenci.setKayitTarihi(extractValue(ogrenciXml, "KAYIT_TARIHI"));
                ogrenci.setAyrilisTarihi(extractValue(ogrenciXml, "AYRILIS_TARIHI"));
                ogrenci.setKayitNedeni(extractValue(ogrenciXml, "KAYIT_NEDENI"));
                ogrenci.setOgrenimDurumu(extractValue(ogrenciXml, "OGRENIM_DURUMU"));
                ogrenci.setDanismanUnvan(extractValue(ogrenciXml, "DANISMAN_UNVAN"));
                ogrenci.setDanismanAd(extractValue(ogrenciXml, "DANISMAN_AD"));
                ogrenci.setDanismanSoyad(extractValue(ogrenciXml, "DANISMAN_SOYAD"));
                ogrenci.setFakKod(extractValue(ogrenciXml, "FAK_KOD"));
                ogrenci.setBolumAd(extractValue(ogrenciXml, "BOLUM_AD"));
                ogrenci.setProgramAd(extractValue(ogrenciXml, "PROGRAM_AD"));
                ogrenci.setDurumu(extractValue(ogrenciXml, "DURUMU"));
                ogrenci.setSinif(extractValue(ogrenciXml, "SINIF"));
                ogrenci.setEposta1(extractValue(ogrenciXml, "EPOSTA1"));
                ogrenci.setEposta2(extractValue(ogrenciXml, "EPOSTA2"));
                ogrenci.setGsm1(extractValue(ogrenciXml, "GSM1"));
                ogrenci.setOgrAdres(extractValue(ogrenciXml, "OGR_ADRES"));
                ogrenci.setOgrPostaKod(extractValue(ogrenciXml, "OGR_POSTA_KOD"));
                ogrenci.setOgrAdresIl(extractValue(ogrenciXml, "OGR_ADRES_IL"));
                ogrenci.setOgrAdresIlce(extractValue(ogrenciXml, "OGR_ADRES_ILCE"));
                ogrenci.setFotoUrl(extractValue(ogrenciXml, "FOTO_URL"));
                ogrenci.setKimlikIlAd(extractValue(ogrenciXml, "KIMLIK_IL_AD"));
                ogrenci.setKimlikIlceAd(extractValue(ogrenciXml, "KIMLIK_ILCE_AD"));
                ogrenci.setAnaAd(extractValue(ogrenciXml, "ANA_AD"));
                ogrenci.setBabaAd(extractValue(ogrenciXml, "BABA_AD"));
                ogrenci.setCiltNo(extractValue(ogrenciXml, "CILT_NO"));
                ogrenci.setSiraNo(extractValue(ogrenciXml, "SIRA_NO"));
                ogrenci.setAileSiraNo(extractValue(ogrenciXml, "AILE_SIRANO"));
                ogrenci.setMahalleKoy(extractValue(ogrenciXml, "MAHALLE_KOY"));
                ogrenci.setOgrenimTip(extractValue(ogrenciXml, "OGRENIM_TIP"));
                ogrenci.setOgrenimTuru(extractValue(ogrenciXml, "OGRENIM_TURU"));
                ogrenci.setDisiplinCeza(extractValue(ogrenciXml, "DISIPLIN_CEZA"));
                ogrenci.setDiplomaNo(extractValue(ogrenciXml, "DIPLOMA_NO"));
                ogrenci.setMezuniyetTarihi(extractValue(ogrenciXml, "MEZUNIYET_TARIHI"));
                ogrenci.setMezun(extractValue(ogrenciXml, "MEZUN"));
                ogrenci.setFakulteAd(extractValue(ogrenciXml, "FAKULTE_AD"));
                ogrenci.setYoksisbirimId(extractValue(ogrenciXml, "YOKSIS_BIRIM_ID"));
                ogrenci.setOgrenciDegisiklikTarihi(extractValue(ogrenciXml, "OGRENCI_DEGISIKLIK_TARIHI"));

                if (ogrenci.getOgrenciNo() != null && !ogrenci.getOgrenciNo().isEmpty()) {
                    ogrenciler.add(ogrenci);
                }
            }

            logger.info("Toplam {} aktif öğrenci parse edildi", ogrenciler.size());
            
        } catch (Exception e) {
            logger.error("Aktif öğrenci parsing hatası: {}", e.getMessage(), e);
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
