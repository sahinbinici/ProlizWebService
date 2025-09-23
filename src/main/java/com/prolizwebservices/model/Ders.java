package com.prolizwebservices.model;

/**
 * Uzaktan eğitim ders bilgilerini temsil eder
 */
public class Ders {
    
    private String dersHarId;           // DERS_HAR_ID
    private String donemAd;             // DONEM_AD
    private String donemId;             // DONEM_ID
    private String dersKodu;            // DERS_KODU
    private String dersAdi;             // DERS_ADI
    private String dersAdiEn;           // DERS_ADI_EN
    private String dersYukuTeorik;      // DERS_YUKU_TEORIK
    private String dersYukuUygulama;    // DERS_YUKU_UYGULAMA
    private String fakAd;               // FAK_AD
    private String bolAd;               // BOL_AD
    private String progAd;              // PROG_AD
    private String dersSubeKod;         // DERS_SUBE_KOD
    private String akts;                // AKTS
    private String kredi;               // KREDI
    private String sinif;               // SINIF
    private String ogretimElemani;      // OGRETIM_ELEMANI
    private String ogretimElemaniTC;    // OGRETIM_ELEMANI_TC

    // Constructors
    public Ders() {}

    public Ders(String dersHarId, String dersKodu, String dersAdi) {
        this.dersHarId = dersHarId;
        this.dersKodu = dersKodu;
        this.dersAdi = dersAdi;
    }

    // Getters and Setters
    public String getDersHarId() { return dersHarId; }
    public void setDersHarId(String dersHarId) { this.dersHarId = dersHarId; }

    public String getDonemAd() { return donemAd; }
    public void setDonemAd(String donemAd) { this.donemAd = donemAd; }

    public String getDonemId() { return donemId; }
    public void setDonemId(String donemId) { this.donemId = donemId; }

    public String getDersKodu() { return dersKodu; }
    public void setDersKodu(String dersKodu) { this.dersKodu = dersKodu; }

    public String getDersAdi() { return dersAdi; }
    public void setDersAdi(String dersAdi) { this.dersAdi = dersAdi; }

    public String getDersAdiEn() { return dersAdiEn; }
    public void setDersAdiEn(String dersAdiEn) { this.dersAdiEn = dersAdiEn; }

    public String getDersYukuTeorik() { return dersYukuTeorik; }
    public void setDersYukuTeorik(String dersYukuTeorik) { this.dersYukuTeorik = dersYukuTeorik; }

    public String getDersYukuUygulama() { return dersYukuUygulama; }
    public void setDersYukuUygulama(String dersYukuUygulama) { this.dersYukuUygulama = dersYukuUygulama; }

    public String getFakAd() { return fakAd; }
    public void setFakAd(String fakAd) { this.fakAd = fakAd; }

    public String getBolAd() { return bolAd; }
    public void setBolAd(String bolAd) { this.bolAd = bolAd; }

    public String getProgAd() { return progAd; }
    public void setProgAd(String progAd) { this.progAd = progAd; }

    public String getDersSubeKod() { return dersSubeKod; }
    public void setDersSubeKod(String dersSubeKod) { this.dersSubeKod = dersSubeKod; }

    public String getAkts() { return akts; }
    public void setAkts(String akts) { this.akts = akts; }

    public String getKredi() { return kredi; }
    public void setKredi(String kredi) { this.kredi = kredi; }

    public String getSinif() { return sinif; }
    public void setSinif(String sinif) { this.sinif = sinif; }

    public String getOgretimElemani() { return ogretimElemani; }
    public void setOgretimElemani(String ogretimElemani) { this.ogretimElemani = ogretimElemani; }

    public String getOgretimElemaniTC() { return ogretimElemaniTC; }
    public void setOgretimElemaniTC(String ogretimElemaniTC) { this.ogretimElemaniTC = ogretimElemaniTC; }

    @Override
    public String toString() {
        return "Ders{" +
                "dersHarId='" + dersHarId + '\'' +
                ", dersKodu='" + dersKodu + '\'' +
                ", dersAdi='" + dersAdi + '\'' +
                ", fakAd='" + fakAd + '\'' +
                ", progAd='" + progAd + '\'' +
                '}';
    }
}
