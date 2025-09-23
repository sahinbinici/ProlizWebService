package com.prolizwebservices.model;

/**
 * Öğretim elemanı bilgilerini temsil eder
 */
public class OgretimElemani {
    
    private String tcKimlikNo;      // TC_KIMLIK_NO
    private String sicilNo;         // SICIL_NO  
    private String adi;             // ADI
    private String soyadi;          // SOYADI
    private String unvan;           // UNVAN
    private String personelTip;     // PERSONEL_TIP
    private String fakAd;           // FAK_AD
    private String bolAd;           // BOL_AD
    private String progAd;          // PROG_AD
    private String ePosta;          // E_POSTA

    // Constructors
    public OgretimElemani() {}

    public OgretimElemani(String tcKimlikNo, String sicilNo, String adi, String soyadi) {
        this.tcKimlikNo = tcKimlikNo;
        this.sicilNo = sicilNo;
        this.adi = adi;
        this.soyadi = soyadi;
    }

    // Getters and Setters
    public String getTcKimlikNo() { return tcKimlikNo; }
    public void setTcKimlikNo(String tcKimlikNo) { this.tcKimlikNo = tcKimlikNo; }

    public String getSicilNo() { return sicilNo; }
    public void setSicilNo(String sicilNo) { this.sicilNo = sicilNo; }

    public String getAdi() { return adi; }
    public void setAdi(String adi) { this.adi = adi; }

    public String getSoyadi() { return soyadi; }
    public void setSoyadi(String soyadi) { this.soyadi = soyadi; }

    public String getUnvan() { return unvan; }
    public void setUnvan(String unvan) { this.unvan = unvan; }

    public String getPersonelTip() { return personelTip; }
    public void setPersonelTip(String personelTip) { this.personelTip = personelTip; }

    public String getFakAd() { return fakAd; }
    public void setFakAd(String fakAd) { this.fakAd = fakAd; }

    public String getBolAd() { return bolAd; }
    public void setBolAd(String bolAd) { this.bolAd = bolAd; }

    public String getProgAd() { return progAd; }
    public void setProgAd(String progAd) { this.progAd = progAd; }

    public String getePosta() { return ePosta; }
    public void setePosta(String ePosta) { this.ePosta = ePosta; }

    public String getAdSoyad() {
        return (adi != null ? adi : "") + " " + (soyadi != null ? soyadi : "");
    }

    @Override
    public String toString() {
        return "OgretimElemani{" +
                "tcKimlikNo='" + tcKimlikNo + '\'' +
                ", sicilNo='" + sicilNo + '\'' +
                ", adi='" + adi + '\'' +
                ", soyadi='" + soyadi + '\'' +
                ", unvan='" + unvan + '\'' +
                ", fakAd='" + fakAd + '\'' +
                '}';
    }
}
