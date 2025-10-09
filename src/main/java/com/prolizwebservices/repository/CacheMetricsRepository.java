package com.prolizwebservices.repository;

import com.prolizwebservices.entity.CacheMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CacheMetricsRepository extends JpaRepository<CacheMetrics, Long> {

    /**
     * Belirli bir tarih ve servis için metrik bul
     */
    Optional<CacheMetrics> findByMetricDateAndServiceMethod(LocalDateTime date, String serviceMethod);

    /**
     * Belirli tarih aralığındaki metrikleri getir
     */
    @Query("SELECT cm FROM CacheMetrics cm WHERE cm.metricDate BETWEEN :start AND :end ORDER BY cm.metricDate DESC")
    List<CacheMetrics> findMetricsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Servis metoduna göre metrikleri getir
     */
    List<CacheMetrics> findByServiceMethodOrderByMetricDateDesc(String serviceMethod);

    /**
     * En son metrikleri getir
     */
    @Query("SELECT cm FROM CacheMetrics cm ORDER BY cm.metricDate DESC")
    List<CacheMetrics> findLatestMetrics();
}
