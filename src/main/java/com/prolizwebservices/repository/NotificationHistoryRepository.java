package com.prolizwebservices.repository;

import com.prolizwebservices.entity.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for NotificationHistory entity
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    
    List<NotificationHistory> findByAcademicIdOrderByCreatedAtDesc(String academicId);
    
    Page<NotificationHistory> findByAcademicIdOrderByCreatedAtDesc(String academicId, Pageable pageable);
    
    List<NotificationHistory> findByLessonIdOrderByCreatedAtDesc(String lessonId);
}
