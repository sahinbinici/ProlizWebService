package com.prolizwebservices.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.prolizwebservices.service.DataCacheService;

/**
 * Health check and basic status endpoint
 */
@RestController
@RequestMapping("/")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://193.140.136.26:8080"}, 
            methods = {RequestMethod.GET, RequestMethod.POST}, 
            allowCredentials = "false")
public class HealthController {

    @Autowired
    private DataCacheService cacheService;

    /**
     * Root endpoint - Basic health check
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("service", "ProlizWebServices");
            status.put("status", "UP");
            status.put("timestamp", LocalDateTime.now());
            status.put("message", "Service is running");
            
            // Cache durumu (güvenli kontrol)
            if (cacheService != null) {
                status.put("cacheInitialized", cacheService.isInitialized());
                status.put("totalCourses", cacheService.getAllDersler().size());
                
                Map<String, Object> progressiveStatus = cacheService.getProgressiveLoadingStatus();
                status.put("cacheProgress", progressiveStatus.get("progressPercent") + "%");
            } else {
                status.put("cacheInitialized", false);
                status.put("totalCourses", 0);
                status.put("cacheProgress", "0%");
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("service", "ProlizWebServices");
            status.put("status", "ERROR");
            status.put("timestamp", LocalDateTime.now());
            status.put("error", e.getMessage());
            status.put("message", "Service has issues");
            
            return ResponseEntity.status(500).body(status);
        }
    }

    /**
     * Simple health endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "ProlizWebServices");
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }

    /**
     * API info endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "ProlizWebServices");
        info.put("description", "SOAP to REST adapter for Gaziantep University Student Information System");
        info.put("version", "1.0.0");
        info.put("documentation", "/swagger-ui.html");
        info.put("apiDocs", "/api-docs");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("Health Check", "/health");
        endpoints.put("API Documentation", "/swagger-ui.html"); 
        endpoints.put("Cache Status", "/api/data/cache/status");
        endpoints.put("Progressive Status", "/api/data/cache/progressive-status");
        endpoints.put("Faculties", "/api/data/fakulteler");
        endpoints.put("Student Courses", "/api/data/ogrenci/{studentNo}/dersler");
        
        info.put("availableEndpoints", endpoints);
        
        return ResponseEntity.ok(info);
    }
}
