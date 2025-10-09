package com.prolizwebservices.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Notification History Entity
 * Stores history of sent notifications
 */
@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_academic", columnList = "academic_id"),
    @Index(name = "idx_lesson", columnList = "lesson_id")
})
@Data
public class NotificationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "academic_id", nullable = false, length = 50)
    private String academicId;
    
    @Column(name = "lesson_id", length = 50)
    private String lessonId;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
    
    @Column(name = "recipient_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RecipientType recipientType;
    
    @Column(name = "class_id", length = 20)
    private String classId;
    
    @Column(name = "recipient_count")
    private Integer recipientCount;
    
    @Column(name = "sent_count")
    private Integer sentCount;
    
    @Column(name = "failed_count")
    private Integer failedCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum RecipientType {
        ALL, CLASS, INDIVIDUAL
    }
}
