package com.prolizwebservices.controller;

import com.prolizwebservices.entity.CacheMetrics;
import com.prolizwebservices.entity.ServiceDependency;
import com.prolizwebservices.repository.CacheMetricsRepository;
import com.prolizwebservices.service.HybridCacheService;
import com.prolizwebservices.service.ServiceDependencyManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache yönetimi ve monitoring için controller
 */
@RestController
@RequestMapping("/api/cache-management")
@CrossOrigin(
    origins = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowedHeaders = "*",
    allowCredentials = "false",
    maxAge = 3600
)
@Tag(name = "Cache Management", description = "Hybrid cache management and monitoring endpoints")
public class CacheManagementController {

    @Autowired
    private HybridCacheService cacheService;

    @Autowired
    private ServiceDependencyManager dependencyManager;

    @Autowired
    private CacheMetricsRepository metricsRepository;

    /**
     * Cache istatistiklerini getir
     */
    @Operation(
        summary = "Get Cache Statistics",
        description = "Returns comprehensive cache statistics including hit rates, sizes, and performance metrics"
    )
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        // Hybrid cache istatistikleri
        HybridCacheService.CacheStatistics stats = cacheService.getStatistics();
        response.put("cacheStatistics", Map.of(
            "totalCachedItems", stats.getTotalCachedItems(),
            "totalCacheSize", stats.getTotalCacheSize() != null ? stats.getTotalCacheSize() : 0,
            "validCaches", stats.getValidCaches(),
            "expiredCaches", stats.getExpiredCaches(),
            "totalHits", stats.getTotalHits(),
            "totalMisses", stats.getTotalMisses(),
            "hitRate", String.format("%.2f%%", stats.getHitRate())
        ));
        
        // Son 7 günün metrikleri
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<CacheMetrics> recentMetrics = metricsRepository.findMetricsBetween(weekAgo, LocalDateTime.now());
        response.put("recentMetrics", recentMetrics);
        
        // Bağımlılık istatistikleri
        Map<String, Object> depStats = dependencyManager.getDependencyStatistics();
        response.put("dependencyStatistics", depStats);
        
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Belirli bir cache'i invalidate et
     */
    @Operation(
        summary = "Invalidate Cache",
        description = "Invalidates a specific cache entry across all layers (Redis, Disk, Database)"
    )
    @DeleteMapping("/invalidate")
    public ResponseEntity<Map<String, Object>> invalidateCache(
            @Parameter(description = "Cache key to invalidate", required = true)
            @RequestParam String cacheKey) {
        
        cacheService.invalidate(cacheKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache invalidated successfully");
        response.put("cacheKey", cacheKey);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Servis metoduna göre cache'leri invalidate et
     */
    @Operation(
        summary = "Invalidate Cache by Service Method",
        description = "Invalidates all cache entries for a specific service method"
    )
    @DeleteMapping("/invalidate/service/{serviceMethod}")
    public ResponseEntity<Map<String, Object>> invalidateCacheByService(
            @Parameter(description = "Service method name", required = true)
            @PathVariable String serviceMethod) {
        
        cacheService.invalidateByServiceMethod(serviceMethod);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Service method caches invalidated successfully");
        response.put("serviceMethod", serviceMethod);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cascade invalidation - bağımlı servisleri de invalidate et
     */
    @Operation(
        summary = "Cascade Invalidate",
        description = "Invalidates cache for a service and all its dependent services"
    )
    @DeleteMapping("/invalidate/cascade/{serviceMethod}")
    public ResponseEntity<Map<String, Object>> cascadeInvalidate(
            @Parameter(description = "Service method name", required = true)
            @PathVariable String serviceMethod) {
        
        List<String> toInvalidate = dependencyManager.getCascadeInvalidationList(serviceMethod);
        
        for (String service : toInvalidate) {
            cacheService.invalidateByServiceMethod(service);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cascade invalidation completed");
        response.put("invalidatedServices", toInvalidate);
        response.put("totalInvalidated", toInvalidate.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Servis bağımlılıklarını listele
     */
    @Operation(
        summary = "List Service Dependencies",
        description = "Returns all active service dependencies"
    )
    @GetMapping("/dependencies")
    public ResponseEntity<List<ServiceDependency>> getDependencies() {
        List<ServiceDependency> dependencies = dependencyManager.getAllDependencies();
        return ResponseEntity.ok(dependencies);
    }

    /**
     * Bağımlılık grafiğini getir
     */
    @Operation(
        summary = "Get Dependency Graph",
        description = "Returns service dependency graph in Mermaid format"
    )
    @GetMapping("/dependencies/graph")
    public ResponseEntity<Map<String, Object>> getDependencyGraph() {
        String mermaidGraph = dependencyManager.generateDependencyGraph();
        
        Map<String, Object> response = new HashMap<>();
        response.put("format", "mermaid");
        response.put("graph", mermaidGraph);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Bağımlılık zincirini çöz
     */
    @Operation(
        summary = "Resolve Dependency Chain",
        description = "Returns the execution order for a service and its dependencies"
    )
    @GetMapping("/dependencies/resolve/{serviceMethod}")
    public ResponseEntity<Map<String, Object>> resolveDependencyChain(
            @Parameter(description = "Service method name", required = true)
            @PathVariable String serviceMethod) {
        
        List<String> executionOrder = dependencyManager.resolveDependencyChain(serviceMethod);
        
        Map<String, Object> response = new HashMap<>();
        response.put("serviceMethod", serviceMethod);
        response.put("executionOrder", executionOrder);
        response.put("totalSteps", executionOrder.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Yeni bağımlılık ekle
     */
    @Operation(
        summary = "Add Service Dependency",
        description = "Creates a new service dependency relationship"
    )
    @PostMapping("/dependencies")
    public ResponseEntity<ServiceDependency> addDependency(
            @Parameter(description = "Parent service name", required = true)
            @RequestParam String parentService,
            @Parameter(description = "Child service name", required = true)
            @RequestParam String childService,
            @Parameter(description = "Parent field name", required = true)
            @RequestParam String parentField,
            @Parameter(description = "Child parameter name", required = true)
            @RequestParam String childParameter,
            @Parameter(description = "Description")
            @RequestParam(required = false) String description,
            @Parameter(description = "Priority (1 = highest)")
            @RequestParam(required = false) Integer priority) {
        
        ServiceDependency dependency = dependencyManager.addDependency(
            parentService, childService, parentField, childParameter, description, priority
        );
        
        return ResponseEntity.ok(dependency);
    }

    /**
     * Bağımlılığı devre dışı bırak
     */
    @Operation(
        summary = "Disable Dependency",
        description = "Disables a service dependency"
    )
    @PutMapping("/dependencies/{id}/disable")
    public ResponseEntity<Map<String, Object>> disableDependency(
            @Parameter(description = "Dependency ID", required = true)
            @PathVariable Long id) {
        
        dependencyManager.disableDependency(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Dependency disabled successfully");
        response.put("dependencyId", id);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cache metrikleri - son 24 saat
     */
    @Operation(
        summary = "Get Recent Cache Metrics",
        description = "Returns cache metrics for the last 24 hours"
    )
    @GetMapping("/metrics/recent")
    public ResponseEntity<List<CacheMetrics>> getRecentMetrics() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<CacheMetrics> metrics = metricsRepository.findMetricsBetween(yesterday, LocalDateTime.now());
        return ResponseEntity.ok(metrics);
    }

    /**
     * Servis metoduna göre metrikler
     */
    @Operation(
        summary = "Get Metrics by Service Method",
        description = "Returns cache metrics for a specific service method"
    )
    @GetMapping("/metrics/service/{serviceMethod}")
    public ResponseEntity<List<CacheMetrics>> getMetricsByService(
            @Parameter(description = "Service method name", required = true)
            @PathVariable String serviceMethod) {
        
        List<CacheMetrics> metrics = metricsRepository.findByServiceMethodOrderByMetricDateDesc(serviceMethod);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Cache health check
     */
    @Operation(
        summary = "Cache Health Check",
        description = "Returns cache system health status"
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            HybridCacheService.CacheStatistics stats = cacheService.getStatistics();
            
            health.put("status", "UP");
            health.put("cacheEnabled", true);
            health.put("hitRate", stats.getHitRate());
            health.put("totalItems", stats.getTotalCachedItems());
            
            // Health score (0-100)
            double healthScore = 100.0;
            if (stats.getHitRate() < 50) healthScore -= 30;
            if (stats.getExpiredCaches() > stats.getValidCaches() * 0.2) healthScore -= 20;
            
            health.put("healthScore", healthScore);
            health.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
    
    // ============================================
    // CACHE STRATEGY MANAGEMENT ENDPOINTS
    // ============================================
    
    /**
     * Mevcut cache stratejisini getir
     */
    @Operation(
        summary = "Get Cache Strategy",
        description = "Returns current cache strategy configuration"
    )
    @GetMapping("/strategy")
    public ResponseEntity<Map<String, Object>> getCacheStrategy() {
        HybridCacheService.CacheStrategyInfo strategy = cacheService.getCacheStrategy();
        
        Map<String, Object> response = new HashMap<>();
        response.put("strategyName", strategy.getStrategyName());
        response.put("redisEnabled", strategy.isRedisEnabled());
        response.put("diskEnabled", strategy.isDiskEnabled());
        response.put("databaseEnabled", strategy.isDatabaseEnabled());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cache stratejisini değiştir
     */
    @Operation(
        summary = "Change Cache Strategy",
        description = "Changes the cache strategy at runtime. Available strategies: MEMORY_ONLY, FULL_PERSISTENCE, DISK_AND_DATABASE, REDIS_ONLY, DISK_ONLY, DATABASE_ONLY, REDIS_AND_DATABASE"
    )
    @PutMapping("/strategy")
    public ResponseEntity<Map<String, Object>> changeCacheStrategy(
            @Parameter(description = "Strategy name", required = true)
            @RequestParam String strategy) {
        
        cacheService.setCacheStrategy(strategy);
        
        HybridCacheService.CacheStrategyInfo newStrategy = cacheService.getCacheStrategy();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache strategy changed successfully");
        response.put("newStrategy", newStrategy.getStrategyName());
        response.put("redisEnabled", newStrategy.isRedisEnabled());
        response.put("diskEnabled", newStrategy.isDiskEnabled());
        response.put("databaseEnabled", newStrategy.isDatabaseEnabled());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Redis cache'i aç/kapat
     */
    @Operation(
        summary = "Toggle Redis Cache",
        description = "Enable or disable Redis cache layer"
    )
    @PutMapping("/strategy/redis")
    public ResponseEntity<Map<String, Object>> toggleRedis(
            @Parameter(description = "Enable Redis", required = true)
            @RequestParam boolean enabled) {
        
        cacheService.setRedisEnabled(enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Redis cache " + (enabled ? "enabled" : "disabled"));
        response.put("redisEnabled", enabled);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Disk cache'i aç/kapat
     */
    @Operation(
        summary = "Toggle Disk Cache",
        description = "Enable or disable Disk cache layer"
    )
    @PutMapping("/strategy/disk")
    public ResponseEntity<Map<String, Object>> toggleDisk(
            @Parameter(description = "Enable Disk", required = true)
            @RequestParam boolean enabled) {
        
        cacheService.setDiskEnabled(enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Disk cache " + (enabled ? "enabled" : "disabled"));
        response.put("diskEnabled", enabled);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Database cache'i aç/kapat
     */
    @Operation(
        summary = "Toggle Database Cache",
        description = "Enable or disable Database cache layer"
    )
    @PutMapping("/strategy/database")
    public ResponseEntity<Map<String, Object>> toggleDatabase(
            @Parameter(description = "Enable Database", required = true)
            @RequestParam boolean enabled) {
        
        cacheService.setDatabaseEnabled(enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Database cache " + (enabled ? "enabled" : "disabled"));
        response.put("databaseEnabled", enabled);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Tüm cache katmanlarını özelleştir
     */
    @Operation(
        summary = "Customize All Cache Layers",
        description = "Enable or disable specific cache layers"
    )
    @PutMapping("/strategy/custom")
    public ResponseEntity<Map<String, Object>> customizeCacheLayers(
            @Parameter(description = "Enable Redis") @RequestParam(required = false, defaultValue = "true") boolean redis,
            @Parameter(description = "Enable Disk") @RequestParam(required = false, defaultValue = "true") boolean disk,
            @Parameter(description = "Enable Database") @RequestParam(required = false, defaultValue = "true") boolean database) {
        
        cacheService.setAllCacheLayers(redis, disk, database);
        
        HybridCacheService.CacheStrategyInfo strategy = cacheService.getCacheStrategy();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache layers customized successfully");
        response.put("strategyName", strategy.getStrategyName());
        response.put("redisEnabled", redis);
        response.put("diskEnabled", disk);
        response.put("databaseEnabled", database);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
