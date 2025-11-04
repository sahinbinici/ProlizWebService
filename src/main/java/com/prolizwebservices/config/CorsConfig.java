package com.prolizwebservices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;

/**
 * Global CORS Configuration
 * Swagger UI ve tüm API endpoint'leri için CORS ayarları
 *
 * NOT: SimpleCorsFilter ile birlikte çalışıyor (iki katmanlı koruma)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedOrigins("http://193.140.136.26:8084", "http://193.140.136.26", "https://193.140.136.26:8084", "https://193.140.136.26", "*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * CORS Filter Bean - Lower Priority
     * SimpleCorsFilter'dan sonra çalışır (yedek katman)
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Tüm origin'lere izin ver
        config.setAllowCredentials(false);
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedOrigins(Arrays.asList("http://193.140.136.26:8084", "http://193.140.136.26", "https://193.140.136.26:8084", "https://193.140.136.26", "*"));

        // Tüm header'lara izin ver
        config.setAllowedHeaders(Collections.singletonList("*"));

        // Response header'ları expose et
        config.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers",
            "Access-Control-Max-Age",
            "Access-Control-Request-Headers",
            "Access-Control-Request-Method",
            "Content-Type",
            "Content-Length",
            "Authorization"
        ));

        // Tüm HTTP metodlarına izin ver
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // Preflight cache süresi
        config.setMaxAge(3600L);

        // Tüm path'lere uygula
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}