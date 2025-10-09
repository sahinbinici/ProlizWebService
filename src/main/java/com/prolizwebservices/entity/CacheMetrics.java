package com.prolizwebservices.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cache performans metriklerini saklayan entity
 */
@Entity
@Table(name = "cache_metrics", indexes = {
    @Index(name = "idx_metric_date", columnList = "metricDate"),
    @Index(name = "idx_service_method", columnList = "serviceMethod")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Metrik tarihi
     */
    @Column(nullable = false)
    private LocalDateTime metricDate;

    /**
     * Servis metodu
     */
    @Column(nullable = false, length = 100)
    private String serviceMethod;

    /**
     * Cache hit sayısı
     */
    @Column(nullable = false)
    private Long cacheHits = 0L;

    /**
     * Cache miss sayısı
     */
    @Column(nullable = false)
    private Long cacheMisses = 0L;

    /**
     * Redis hit sayısı
     */
    @Column(nullable = false)
    private Long redisHits = 0L;

    /**
     * Disk cache hit sayısı
     */
    @Column(nullable = false)
    private Long diskHits = 0L;

    /**
     * Database hit sayısı
     */
    @Column(nullable = false)
    private Long databaseHits = 0L;

    /**
     * SOAP çağrı sayısı
     */
    @Column(nullable = false)
    private Long soapCalls = 0L;

    /**
     * Ortalama yanıt süresi (ms)
     */
    @Column(nullable = false)
    private Double avgResponseTime = 0.0;

    /**
     * Toplam veri boyutu (bytes)
     */
    @Column(nullable = false)
    private Long totalDataSize = 0L;

    /**
     * Cache hit oranı (%)
     */
    public double getHitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0 ? 0.0 : (cacheHits * 100.0) / total;
    }
}
