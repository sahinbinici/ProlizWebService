-- Push Notification System Database Schema
-- For MariaDB/MySQL

-- Create notification database (if not exists)
CREATE DATABASE IF NOT EXISTS notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE notification;

-- Notification Tokens Table
CREATE TABLE IF NOT EXISTS notification_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    platform VARCHAR(20),
    device_id VARCHAR(100),
    os_version VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, user_type),
    INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification History Table
CREATE TABLE IF NOT EXISTS notification_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    academic_id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    class_id VARCHAR(20),
    recipient_count INT DEFAULT 0,
    sent_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_academic (academic_id),
    INDEX idx_lesson (lesson_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample Queries

-- Get all tokens for a user
-- SELECT * FROM notification_tokens WHERE user_id = '20180001234' AND user_type = 'STUDENT';

-- Get notification history for an academic
-- SELECT * FROM notification_history WHERE academic_id = '12345' ORDER BY created_at DESC LIMIT 50;

-- Get token statistics
-- SELECT user_type, COUNT(*) as token_count FROM notification_tokens GROUP BY user_type;

-- Get notification statistics by academic
-- SELECT 
--     academic_id,
--     COUNT(*) as total_notifications,
--     SUM(sent_count) as total_sent,
--     SUM(failed_count) as total_failed,
--     SUM(recipient_count) as total_recipients
-- FROM notification_history 
-- GROUP BY academic_id;

-- Clean up old tokens (optional maintenance query)
-- DELETE FROM notification_tokens WHERE updated_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Clean up old history (optional maintenance query)
-- DELETE FROM notification_history WHERE created_at < DATE_SUB(NOW(), INTERVAL 365 DAY);
