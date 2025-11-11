package com.prolizwebservices.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aktif öğrenci detay bilgileri")
public class AktifOgrenci {

    @JsonProperty("AKTIFMI")
    @Schema(description = "Öğrenci aktif mi", example = "1")
    private String aktifMi;

    @JsonProperty("TC_KIMLIK_NO")
    @Schema(description = "TC kimlik numarası", example = "12345678901")
    private String tcKimlikNo;

    @JsonProperty("OGRENCI_NO")
    @Schema(description = "Öğrenci numarası", example = "20230001")
    private String ogrenciNo;

    @JsonProperty("AD")
    @Schema(description = "Öğrenci adı", example = "Ahmet")
    private String ad;

    @JsonProperty("SOYAD")
    @Schema(description = "Öğrenci soyadı", example = "Yılmaz")
    private String soyad;

    @JsonProperty("UYRUK")
    @Schema(description = "Uyruk", example = "TC")
    private String uyruk;

    @JsonProperty("DOGUM_YERI")
    @Schema(description = "Doğum yeri", example = "Gaziantep")
    private String dogumYeri;

    @JsonProperty("DOGUM_TARIHI")
    @Schema(description = "Doğum tarihi", example = "1995-05-15")
    private String dogumTarihi;

    @JsonProperty("CINSIYET")
    @Schema(description = "Cinsiyet", example = "E")
    private String cinsiyet;

    @JsonProperty("EGITIM_DERECESI")
    @Schema(description = "Eğitim derecesi", example = "Lisans")
    private String egitimDerecesi;

    @JsonProperty("KAYIT_TARIHI")
    @Schema(description = "Kayıt tarihi", example = "2023-09-15")
    private String kayitTarihi;

    @JsonProperty("AYRILIS_TARIHI")
    @Schema(description = "Ayrılış tarihi", example = "")
    private String ayrilisTarihi;

    @JsonProperty("KAYIT_NEDENI")
    @Schema(description = "Kayıt nedeni", example = "YKS")
    private String kayitNedeni;

    @JsonProperty("OGRENIM_DURUMU")
    @Schema(description = "Öğrenim durumu", example = "Aktif")
    private String ogrenimDurumu;

    @JsonProperty("DANISMAN_UNVAN")
    @Schema(description = "Danışman unvanı", example = "Prof. Dr.")
    private String danismanUnvan;

    @JsonProperty("DANISMAN_AD")
    @Schema(description = "Danışman adı", example = "Mehmet")
    private String danismanAd;

    @JsonProperty("DANISMAN_SOYAD")
    @Schema(description = "Danışman soyadı", example = "Kaya")
    private String danismanSoyad;

    @JsonProperty("FAK_KOD")
    @Schema(description = "Fakülte kodu", example = "MUH")
    private String fakKod;

    @JsonProperty("BOLUM_AD")
    @Schema(description = "Bölüm adı", example = "Bilgisayar Mühendisliği")
    private String bolumAd;

    @JsonProperty("PROGRAM_AD")
    @Schema(description = "Program adı", example = "Bilgisayar Mühendisliği")
    private String programAd;

    @JsonProperty("DURUMU")
    @Schema(description = "Durumu", example = "Aktif")
    private String durumu;

    @JsonProperty("SINIF")
    @Schema(description = "Sınıf", example = "3")
    private String sinif;

    @JsonProperty("EPOSTA1")
    @Schema(description = "E-posta 1", example = "ahmet.yilmaz@ogr.gantep.edu.tr")
    private String eposta1;

    @JsonProperty("EPOSTA2")
    @Schema(description = "E-posta 2", example = "ahmet@gmail.com")
    private String eposta2;

    @JsonProperty("GSM1")
    @Schema(description = "GSM numarası", example = "05551234567")
    private String gsm1;

    @JsonProperty("OGR_ADRES")
    @Schema(description = "Öğrenci adresi", example = "Şehitkamil/Gaziantep")
    private String ogrAdres;

    @JsonProperty("OGR_POSTA_KOD")
    @Schema(description = "Posta kodu", example = "27000")
    private String ogrPostaKod;

    @JsonProperty("OGR_ADRES_IL")
    @Schema(description = "Adres ili", example = "Gaziantep")
    private String ogrAdresIl;

    @JsonProperty("OGR_ADRES_ILCE")
    @Schema(description = "Adres ilçesi", example = "Şehitkamil")
    private String ogrAdresIlce;

    @JsonProperty("FOTO_URL")
    @Schema(description = "Fotoğraf URL", example = "")
    private String fotoUrl;

    @JsonProperty("KIMLIK_IL_AD")
    @Schema(description = "Kimlik il adı", example = "Gaziantep")
    private String kimlikIlAd;

    @JsonProperty("KIMLIK_ILCE_AD")
    @Schema(description = "Kimlik ilçe adı", example = "Şehitkamil")
    private String kimlikIlceAd;

    @JsonProperty("ANA_AD")
    @Schema(description = "Ana adı", example = "Fatma")
    private String anaAd;

    @JsonProperty("BABA_AD")
    @Schema(description = "Baba adı", example = "Ali")
    private String babaAd;

    @JsonProperty("CILT_NO")
    @Schema(description = "Cilt numarası", example = "123")
    private String ciltNo;

    @JsonProperty("SIRA_NO")
    @Schema(description = "Sıra numarası", example = "456")
    private String siraNo;

    @JsonProperty("AILE_SIRANO")
    @Schema(description = "Aile sıra numarası", example = "1")
    private String aileSiraNo;

    @JsonProperty("MAHALLE_KOY")
    @Schema(description = "Mahalle/Köy", example = "Merkez Mahallesi")
    private String mahalleKoy;

    @JsonProperty("OGRENIM_TIP")
    @Schema(description = "Öğrenim tipi", example = "Normal")
    private String ogrenimTip;

    @JsonProperty("OGRENIM_TURU")
    @Schema(description = "Öğrenim türü", example = "I. Öğretim")
    private String ogrenimTuru;

    @JsonProperty("DISIPLIN_CEZA")
    @Schema(description = "Disiplin cezası", example = "")
    private String disiplinCeza;

    @JsonProperty("DIPLOMA_NO")
    @Schema(description = "Diploma numarası", example = "")
    private String diplomaNo;

    @JsonProperty("MEZUNIYET_TARIHI")
    @Schema(description = "Mezuniyet tarihi", example = "")
    private String mezuniyetTarihi;

    @JsonProperty("MEZUN")
    @Schema(description = "Mezun mu", example = "0")
    private String mezun;

    @JsonProperty("FAKULTE_AD")
    @Schema(description = "Fakülte adı", example = "Mühendislik Fakültesi")
    private String fakulteAd;

    @JsonProperty("YOKSIS_BIRIM_ID")
    @Schema(description = "YÖKSİS birim ID", example = "123456")
    private String yoksisbirimId;

    @JsonProperty("OGRENCI_DEGISIKLIK_TARIHI")
    @Schema(description = "Öğrenci değişiklik tarihi", example = "2023-09-15")
    private String ogrenciDegisiklikTarihi;

    // Constructors
    public AktifOgrenci() {}

    // Getters and Setters
    public String getAktifMi() {
        return aktifMi;
    }

    public void setAktifMi(String aktifMi) {
        this.aktifMi = aktifMi;
    }

    public String getTcKimlikNo() {
        return tcKimlikNo;
    }

    public void setTcKimlikNo(String tcKimlikNo) {
        this.tcKimlikNo = tcKimlikNo;
    }

    public String getOgrenciNo() {
        return ogrenciNo;
    }

    public void setOgrenciNo(String ogrenciNo) {
        this.ogrenciNo = ogrenciNo;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        this.ad = ad;
    }

    public String getSoyad() {
        return soyad;
    }

    public void setSoyad(String soyad) {
        this.soyad = soyad;
    }

    public String getUyruk() {
        return uyruk;
    }

    public void setUyruk(String uyruk) {
        this.uyruk = uyruk;
    }

    public String getDogumYeri() {
        return dogumYeri;
    }

    public void setDogumYeri(String dogumYeri) {
        this.dogumYeri = dogumYeri;
    }

    public String getDogumTarihi() {
        return dogumTarihi;
    }

    public void setDogumTarihi(String dogumTarihi) {
        this.dogumTarihi = dogumTarihi;
    }

    public String getCinsiyet() {
        return cinsiyet;
    }

    public void setCinsiyet(String cinsiyet) {
        this.cinsiyet = cinsiyet;
    }

    public String getEgitimDerecesi() {
        return egitimDerecesi;
    }

    public void setEgitimDerecesi(String egitimDerecesi) {
        this.egitimDerecesi = egitimDerecesi;
    }

    public String getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(String kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    public String getAyrilisTarihi() {
        return ayrilisTarihi;
    }

    public void setAyrilisTarihi(String ayrilisTarihi) {
        this.ayrilisTarihi = ayrilisTarihi;
    }

    public String getKayitNedeni() {
        return kayitNedeni;
    }

    public void setKayitNedeni(String kayitNedeni) {
        this.kayitNedeni = kayitNedeni;
    }

    public String getOgrenimDurumu() {
        return ogrenimDurumu;
    }

    public void setOgrenimDurumu(String ogrenimDurumu) {
        this.ogrenimDurumu = ogrenimDurumu;
    }

    public String getDanismanUnvan() {
        return danismanUnvan;
    }

    public void setDanismanUnvan(String danismanUnvan) {
        this.danismanUnvan = danismanUnvan;
    }

    public String getDanismanAd() {
        return danismanAd;
    }

    public void setDanismanAd(String danismanAd) {
        this.danismanAd = danismanAd;
    }

    public String getDanismanSoyad() {
        return danismanSoyad;
    }

    public void setDanismanSoyad(String danismanSoyad) {
        this.danismanSoyad = danismanSoyad;
    }

    public String getFakKod() {
        return fakKod;
    }

    public void setFakKod(String fakKod) {
        this.fakKod = fakKod;
    }

    public String getBolumAd() {
        return bolumAd;
    }

    public void setBolumAd(String bolumAd) {
        this.bolumAd = bolumAd;
    }

    public String getProgramAd() {
        return programAd;
    }

    public void setProgramAd(String programAd) {
        this.programAd = programAd;
    }

    public String getDurumu() {
        return durumu;
    }

    public void setDurumu(String durumu) {
        this.durumu = durumu;
    }

    public String getSinif() {
        return sinif;
    }

    public void setSinif(String sinif) {
        this.sinif = sinif;
    }

    public String getEposta1() {
        return eposta1;
    }

    public void setEposta1(String eposta1) {
        this.eposta1 = eposta1;
    }

    public String getEposta2() {
        return eposta2;
    }

    public void setEposta2(String eposta2) {
        this.eposta2 = eposta2;
    }

    public String getGsm1() {
        return gsm1;
    }

    public void setGsm1(String gsm1) {
        this.gsm1 = gsm1;
    }

    public String getOgrAdres() {
        return ogrAdres;
    }

    public void setOgrAdres(String ogrAdres) {
        this.ogrAdres = ogrAdres;
    }

    public String getOgrPostaKod() {
        return ogrPostaKod;
    }

    public void setOgrPostaKod(String ogrPostaKod) {
        this.ogrPostaKod = ogrPostaKod;
    }

    public String getOgrAdresIl() {
        return ogrAdresIl;
    }

    public void setOgrAdresIl(String ogrAdresIl) {
        this.ogrAdresIl = ogrAdresIl;
    }

    public String getOgrAdresIlce() {
        return ogrAdresIlce;
    }

    public void setOgrAdresIlce(String ogrAdresIlce) {
        this.ogrAdresIlce = ogrAdresIlce;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getKimlikIlAd() {
        return kimlikIlAd;
    }

    public void setKimlikIlAd(String kimlikIlAd) {
        this.kimlikIlAd = kimlikIlAd;
    }

    public String getKimlikIlceAd() {
        return kimlikIlceAd;
    }

    public void setKimlikIlceAd(String kimlikIlceAd) {
        this.kimlikIlceAd = kimlikIlceAd;
    }

    public String getAnaAd() {
        return anaAd;
    }

    public void setAnaAd(String anaAd) {
        this.anaAd = anaAd;
    }

    public String getBabaAd() {
        return babaAd;
    }

    public void setBabaAd(String babaAd) {
        this.babaAd = babaAd;
    }

    public String getCiltNo() {
        return ciltNo;
    }

    public void setCiltNo(String ciltNo) {
        this.ciltNo = ciltNo;
    }

    public String getSiraNo() {
        return siraNo;
    }

    public void setSiraNo(String siraNo) {
        this.siraNo = siraNo;
    }

    public String getAileSiraNo() {
        return aileSiraNo;
    }

    public void setAileSiraNo(String aileSiraNo) {
        this.aileSiraNo = aileSiraNo;
    }

    public String getMahalleKoy() {
        return mahalleKoy;
    }

    public void setMahalleKoy(String mahalleKoy) {
        this.mahalleKoy = mahalleKoy;
    }

    public String getOgrenimTip() {
        return ogrenimTip;
    }

    public void setOgrenimTip(String ogrenimTip) {
        this.ogrenimTip = ogrenimTip;
    }

    public String getOgrenimTuru() {
        return ogrenimTuru;
    }

    public void setOgrenimTuru(String ogrenimTuru) {
        this.ogrenimTuru = ogrenimTuru;
    }

    public String getDisiplinCeza() {
        return disiplinCeza;
    }

    public void setDisiplinCeza(String disiplinCeza) {
        this.disiplinCeza = disiplinCeza;
    }

    public String getDiplomaNo() {
        return diplomaNo;
    }

    public void setDiplomaNo(String diplomaNo) {
        this.diplomaNo = diplomaNo;
    }

    public String getMezuniyetTarihi() {
        return mezuniyetTarihi;
    }

    public void setMezuniyetTarihi(String mezuniyetTarihi) {
        this.mezuniyetTarihi = mezuniyetTarihi;
    }

    public String getMezun() {
        return mezun;
    }

    public void setMezun(String mezun) {
        this.mezun = mezun;
    }

    public String getFakulteAd() {
        return fakulteAd;
    }

    public void setFakulteAd(String fakulteAd) {
        this.fakulteAd = fakulteAd;
    }

    public String getYoksisbirimId() {
        return yoksisbirimId;
    }

    public void setYoksisbirimId(String yoksisbirimId) {
        this.yoksisbirimId = yoksisbirimId;
    }

    public String getOgrenciDegisiklikTarihi() {
        return ogrenciDegisiklikTarihi;
    }

    public void setOgrenciDegisiklikTarihi(String ogrenciDegisiklikTarihi) {
        this.ogrenciDegisiklikTarihi = ogrenciDegisiklikTarihi;
    }

    @Override
    public String toString() {
        return "AktifOgrenci{" +
                "ogrenciNo='" + ogrenciNo + '\'' +
                ", ad='" + ad + '\'' +
                ", soyad='" + soyad + '\'' +
                ", tcKimlikNo='" + tcKimlikNo + '\'' +
                ", bolumAd='" + bolumAd + '\'' +
                ", fakulteAd='" + fakulteAd + '\'' +
                ", sinif='" + sinif + '\'' +
                ", durumu='" + durumu + '\'' +
                '}';
    }
}