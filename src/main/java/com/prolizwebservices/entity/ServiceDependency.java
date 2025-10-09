package com.prolizwebservices.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SOAP servisleri arasındaki bağımlılıkları yöneten entity
 * Örnek: UzaktanEgitimDersleri -> UzaktanEgitimDersiAlanOgrencileri
 */
@Entity
@Table(name = "service_dependencies", indexes = {
    @Index(name = "idx_parent_service", columnList = "parentService"),
    @Index(name = "idx_child_service", columnList = "childService")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Üst servis (veri kaynağı)
     * Örnek: UzaktanEgitimDersleri
     */
    @Column(nullable = false, length = 100)
    private String parentService;

    /**
     * Alt servis (bağımlı servis)
     * Örnek: UzaktanEgitimDersiAlanOgrencileri
     */
    @Column(nullable = false, length = 100)
    private String childService;

    /**
     * Üst servisten alınan alan adı
     * Örnek: DERS_HAR_ID
     */
    @Column(nullable = false, length = 100)
    private String parentFieldName;

    /**
     * Alt servise gönderilen parametre adı
     * Örnek: dersHarID
     */
    @Column(nullable = false, length = 100)
    private String childParameterName;

    /**
     * Bağımlılık açıklaması
     */
    @Column(length = 500)
    private String description;

    /**
     * Bağımlılık önceliği (1 = en yüksek)
     */
    @Column(nullable = false)
    private Integer priority = 1;

    /**
     * Oluşturulma zamanı
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Aktif mi?
     */
    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
