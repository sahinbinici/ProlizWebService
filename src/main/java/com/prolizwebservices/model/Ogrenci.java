package com.prolizwebservices.model;

/**
 * Öğrenci bilgilerini temsil eder
 */
public class Ogrenci {
    
    private String ogrNo;               // OGR_NO
    private String tcKimlikNo;          // TCKIMLIKNO
    private String adi;                 // ADI
    private String soyadi;              // SOYADI
    private String fakulte;             // FAKULTE
    private String bolum;               // BOLUM
    private String program;             // PROGRAM
    private String sinif;               // SINIF
    private String kayitNeden;          // KAYIT_NEDEN
    private String ogrenimDurum;        // OGRENIM_DURUM
    private String dersKredi;           // DERS_KREDI
    private String dersAkts;            // DERS_AKTS
    
    // Ders ile ilişki için
    private String dersHarId;           // Hangi derse kayıtlı
    
    // Constructors
    public Ogrenci() {}

    public Ogrenci(String ogrNo, String tcKimlikNo, String adi, String soyadi) {
        this.ogrNo = ogrNo;
        this.tcKimlikNo = tcKimlikNo;
        this.adi = adi;
        this.soyadi = soyadi;
    }

    // Getters and Setters
    public String getOgrNo() { return ogrNo; }
    public void setOgrNo(String ogrNo) { this.ogrNo = ogrNo; }

    public String getTcKimlikNo() { return tcKimlikNo; }
    public void setTcKimlikNo(String tcKimlikNo) { this.tcKimlikNo = tcKimlikNo; }

    public String getAdi() { return adi; }
    public void setAdi(String adi) { this.adi = adi; }

    public String getSoyadi() { return soyadi; }
    public void setSoyadi(String soyadi) { this.soyadi = soyadi; }

    public String getFakulte() { return fakulte; }
    public void setFakulte(String fakulte) { this.fakulte = fakulte; }

    public String getBolum() { return bolum; }
    public void setBolum(String bolum) { this.bolum = bolum; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getSinif() { return sinif; }
    public void setSinif(String sinif) { this.sinif = sinif; }

    public String getKayitNeden() { return kayitNeden; }
    public void setKayitNeden(String kayitNeden) { this.kayitNeden = kayitNeden; }

    public String getOgrenimDurum() { return ogrenimDurum; }
    public void setOgrenimDurum(String ogrenimDurum) { this.ogrenimDurum = ogrenimDurum; }

    public String getDersKredi() { return dersKredi; }
    public void setDersKredi(String dersKredi) { this.dersKredi = dersKredi; }

    public String getDersAkts() { return dersAkts; }
    public void setDersAkts(String dersAkts) { this.dersAkts = dersAkts; }

    public String getDersHarId() { return dersHarId; }
    public void setDersHarId(String dersHarId) { this.dersHarId = dersHarId; }

    public String getAdSoyad() {
        return (adi != null ? adi : "") + " " + (soyadi != null ? soyadi : "");
    }

    @Override
    public String toString() {
        return "Ogrenci{" +
                "ogrNo='" + ogrNo + '\'' +
                ", tcKimlikNo='" + tcKimlikNo + '\'' +
                ", adi='" + adi + '\'' +
                ", soyadi='" + soyadi + '\'' +
                ", fakulte='" + fakulte + '\'' +
                ", bolum='" + bolum + '\'' +
                ", program='" + program + '\'' +
                ", sinif='" + sinif + '\'' +
                '}';
    }
}
