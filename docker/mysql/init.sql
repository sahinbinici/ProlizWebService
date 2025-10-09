-- MySQL initialization script for ProlizWebServices

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS proliz_cache CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE proliz_cache;

-- Grant privileges
GRANT ALL PRIVILEGES ON proliz_cache.* TO 'proliz'@'%';
FLUSH PRIVILEGES;

-- Create indexes for better performance
SET GLOBAL innodb_buffer_pool_size = 268435456; -- 256MB

-- Optimize MySQL for cache workload
SET GLOBAL max_connections = 200;
SET GLOBAL query_cache_size = 67108864; -- 64MB
SET GLOBAL query_cache_type = 1;
