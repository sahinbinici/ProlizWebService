package com.prolizwebservices.repository;

import com.prolizwebservices.entity.ServiceDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDependencyRepository extends JpaRepository<ServiceDependency, Long> {

    /**
     * Üst servise bağlı tüm alt servisleri bul
     */
    List<ServiceDependency> findByParentServiceAndActiveTrue(String parentService);

    /**
     * Alt servisin üst servislerini bul
     */
    List<ServiceDependency> findByChildServiceAndActiveTrue(String childService);

    /**
     * Belirli bir bağımlılığı bul
     */
    @Query("SELECT sd FROM ServiceDependency sd WHERE sd.parentService = :parent AND sd.childService = :child AND sd.active = true")
    List<ServiceDependency> findDependency(@Param("parent") String parent, @Param("child") String child);

    /**
     * Tüm aktif bağımlılıkları öncelik sırasına göre getir
     */
    List<ServiceDependency> findByActiveTrueOrderByPriorityAsc();
}
