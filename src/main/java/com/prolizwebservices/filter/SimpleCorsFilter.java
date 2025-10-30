package com.prolizwebservices.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Simple CORS Filter
 * En düşük seviyede CORS header'larını ekler
 * Tüm isteklere CORS header'ları ekleyerek Swagger UI sorunlarını çözer
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimpleCorsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCorsFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        String origin = request.getHeader("Origin");
        String requestMethod = request.getMethod();
        String requestURI = request.getRequestURI();
        
        logger.info("CORS Filter - Method: {}, Origin: {}, Path: {}", 
                    requestMethod, origin, requestURI);
        
        // CORS header'larını ekle - MUTLAKA bu sırayla
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control, Pragma");
        response.addHeader("Access-Control-Expose-Headers", "Content-Type, Content-Length, Authorization, X-Total-Count");
        response.addHeader("Access-Control-Max-Age", "3600");
        
        logger.info("CORS Headers added for: {}", requestURI);
        
        // OPTIONS isteği için hemen yanıt dön
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.setContentLength(0);
            logger.info("CORS Preflight handled - Returning 200 OK for: {}", requestURI);
            return;
        }
        
        // Diğer istekleri devam ettir
        chain.doFilter(req, res);
        
        logger.info("CORS Filter completed for: {}", requestURI);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("SimpleCorsFilter initialized - HIGHEST_PRECEDENCE");
    }

    @Override
    public void destroy() {
        logger.info("SimpleCorsFilter destroyed");
    }
}
