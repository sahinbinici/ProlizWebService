package com.prolizwebservices.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SOAP servis yanıtlarını veritabanında saklayan entity
 * Kalıcı cache için kullanılır - sunucu restart olsa bile veriler kaybolmaz
 */
@Entity
@Table(name = "cached_soap_responses", indexes = {
    @Index(name = "idx_service_method", columnList = "serviceMethod"),
    @Index(name = "idx_cache_key", columnList = "cacheKey", unique = true),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedSoapResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SOAP servis metodu (örn: UzaktanEgitimDersleri, UzaktanEgitimDersiAlanOgrencileri)
     */
    @Column(nullable = false, length = 100)
    private String serviceMethod;

    /**
     * Cache anahtarı (method + parametreler kombinasyonu)
     */
    @Column(nullable = false, unique = true, length = 500)
    private String cacheKey;

    /**
     * SOAP yanıtının XML içeriği
     */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String xmlResponse;

    /**
     * Cache oluşturulma zamanı
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Son erişim zamanı (LRU için)
     */
    @Column(nullable = false)
    private LocalDateTime lastAccessedAt;

    /**
     * Cache son kullanma tarihi
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Erişim sayısı (popülerlik için)
     */
    @Column(nullable = false)
    private Long accessCount = 0L;

    /**
     * Yanıt boyutu (byte)
     */
    @Column(nullable = false)
    private Long responseSize;

    /**
     * Cache durumu (VALID, EXPIRED, REFRESHING)
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CacheStatus status = CacheStatus.VALID;

    public enum CacheStatus {
        VALID,      // Geçerli cache
        EXPIRED,    // Süresi dolmuş
        REFRESHING  // Yenileniyor
    }

    /**
     * Cache'e erişim kaydeder
     */
    public void recordAccess() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;
    }

    /**
     * Cache'in süresi dolmuş mu?
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
