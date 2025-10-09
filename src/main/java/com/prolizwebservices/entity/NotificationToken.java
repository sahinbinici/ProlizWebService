package com.prolizwebservices.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Push Notification Token Entity
 * Stores device tokens for push notifications
 */
@Entity
@Table(name = "notification_tokens", indexes = {
    @Index(name = "idx_user", columnList = "user_id,user_type"),
    @Index(name = "idx_token", columnList = "token")
})
@Data
public class NotificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 255)
    private String token;
    
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Column(name = "user_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserType userType;
    
    @Column(length = 20)
    private String platform;
    
    @Column(name = "device_id", length = 100)
    private String deviceId;
    
    @Column(name = "os_version", length = 50)
    private String osVersion;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum UserType {
        STUDENT, ACADEMIC
    }
}
