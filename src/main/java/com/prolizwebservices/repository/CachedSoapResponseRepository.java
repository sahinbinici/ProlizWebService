package com.prolizwebservices.repository;

import com.prolizwebservices.entity.CachedSoapResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CachedSoapResponseRepository extends JpaRepository<CachedSoapResponse, Long> {

    /**
     * Cache key ile cache'i bul
     */
    Optional<CachedSoapResponse> findByCacheKey(String cacheKey);

    /**
     * Servis metoduna göre tüm cache'leri bul
     */
    List<CachedSoapResponse> findByServiceMethod(String serviceMethod);

    /**
     * Süresi dolmuş cache'leri bul
     */
    @Query("SELECT c FROM CachedSoapResponse c WHERE c.expiresAt < :now AND c.status = 'VALID'")
    List<CachedSoapResponse> findExpiredCaches(@Param("now") LocalDateTime now);

    /**
     * Belirli bir tarihten eski cache'leri sil
     */
    @Modifying
    @Query("DELETE FROM CachedSoapResponse c WHERE c.createdAt < :beforeDate")
    void deleteOldCaches(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * En az kullanılan cache'leri bul (LRU)
     */
    @Query("SELECT c FROM CachedSoapResponse c ORDER BY c.lastAccessedAt ASC")
    List<CachedSoapResponse> findLeastRecentlyUsed();

    /**
     * Toplam cache boyutunu hesapla
     */
    @Query("SELECT SUM(c.responseSize) FROM CachedSoapResponse c")
    Long getTotalCacheSize();

    /**
     * Servis metoduna göre cache sayısı
     */
    @Query("SELECT COUNT(c) FROM CachedSoapResponse c WHERE c.serviceMethod = :method")
    Long countByServiceMethod(@Param("method") String method);

    /**
     * Cache durumuna göre sayı
     */
    Long countByStatus(CachedSoapResponse.CacheStatus status);
}
