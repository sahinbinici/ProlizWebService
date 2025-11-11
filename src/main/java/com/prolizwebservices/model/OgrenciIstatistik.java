package com.prolizwebservices.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Öğrenci istatistik bilgileri")
public class OgrenciIstatistik {

    @JsonProperty("OGR_SAY")
    @Schema(description = "Öğrenci sayısı", example = "15000")
    private String ogrenciSayisi;

    @JsonProperty("AKD_SAY")
    @Schema(description = "Akademik personel sayısı", example = "1200")
    private String akademikPersonelSayisi;

    @JsonProperty("IDR_SAY")
    @Schema(description = "İdari personel sayısı", example = "800")
    private String idariPersonelSayisi;

    @JsonProperty("FAK_SAY")
    @Schema(description = "Fakülte sayısı", example = "12")
    private String fakulteSayisi;

    @JsonProperty("MYO_SAY")
    @Schema(description = "Meslek yüksekokulu sayısı", example = "8")
    private String myoSayisi;

    @JsonProperty("YO_SAY")
    @Schema(description = "Yüksekokul sayısı", example = "4")
    private String yuksekOkulSayisi;

    @JsonProperty("ENS_SAY")
    @Schema(description = "Enstitü sayısı", example = "3")
    private String enstituSayisi;

    // Constructors
    public OgrenciIstatistik() {}

    public OgrenciIstatistik(String ogrenciSayisi, String akademikPersonelSayisi, String idariPersonelSayisi,
                           String fakulteSayisi, String myoSayisi, String yuksekOkulSayisi, String enstituSayisi) {
        this.ogrenciSayisi = ogrenciSayisi;
        this.akademikPersonelSayisi = akademikPersonelSayisi;
        this.idariPersonelSayisi = idariPersonelSayisi;
        this.fakulteSayisi = fakulteSayisi;
        this.myoSayisi = myoSayisi;
        this.yuksekOkulSayisi = yuksekOkulSayisi;
        this.enstituSayisi = enstituSayisi;
    }

    // Getters and Setters
    public String getOgrenciSayisi() {
        return ogrenciSayisi;
    }

    public void setOgrenciSayisi(String ogrenciSayisi) {
        this.ogrenciSayisi = ogrenciSayisi;
    }

    public String getAkademikPersonelSayisi() {
        return akademikPersonelSayisi;
    }

    public void setAkademikPersonelSayisi(String akademikPersonelSayisi) {
        this.akademikPersonelSayisi = akademikPersonelSayisi;
    }

    public String getIdariPersonelSayisi() {
        return idariPersonelSayisi;
    }

    public void setIdariPersonelSayisi(String idariPersonelSayisi) {
        this.idariPersonelSayisi = idariPersonelSayisi;
    }

    public String getFakulteSayisi() {
        return fakulteSayisi;
    }

    public void setFakulteSayisi(String fakulteSayisi) {
        this.fakulteSayisi = fakulteSayisi;
    }

    public String getMyoSayisi() {
        return myoSayisi;
    }

    public void setMyoSayisi(String myoSayisi) {
        this.myoSayisi = myoSayisi;
    }

    public String getYuksekOkulSayisi() {
        return yuksekOkulSayisi;
    }

    public void setYuksekOkulSayisi(String yuksekOkulSayisi) {
        this.yuksekOkulSayisi = yuksekOkulSayisi;
    }

    public String getEnstituSayisi() {
        return enstituSayisi;
    }

    public void setEnstituSayisi(String enstituSayisi) {
        this.enstituSayisi = enstituSayisi;
    }

    @Override
    public String toString() {
        return "OgrenciIstatistik{" +
                "ogrenciSayisi='" + ogrenciSayisi + '\'' +
                ", akademikPersonelSayisi='" + akademikPersonelSayisi + '\'' +
                ", idariPersonelSayisi='" + idariPersonelSayisi + '\'' +
                ", fakulteSayisi='" + fakulteSayisi + '\'' +
                ", myoSayisi='" + myoSayisi + '\'' +
                ", yuksekOkulSayisi='" + yuksekOkulSayisi + '\'' +
                ", enstituSayisi='" + enstituSayisi + '\'' +
                '}';
    }
}