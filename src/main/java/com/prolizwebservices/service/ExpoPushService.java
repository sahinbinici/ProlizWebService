package com.prolizwebservices.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for sending push notifications via Expo Push Service
 */
@Service
@Slf4j
public class ExpoPushService {
    
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Send push notifications to multiple devices
     */
    public Map<String, Object> sendPushNotifications(
            List<String> tokens,
            String title,
            String body,
            Map<String, Object> data,
            String channelId) {
        
        if (tokens == null || tokens.isEmpty()) {
            log.warn("No tokens provided for push notification");
            return Map.of("success", false, "error", "No tokens provided");
        }
        
        List<Map<String, Object>> messages = tokens.stream()
            .map(token -> createMessage(token, title, body, data, channelId))
            .collect(Collectors.toList());
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(EXPO_PUSH_URL, request, Map.class);
            
            log.info("Push notification sent to {} devices. Status: {}", tokens.size(), response.getStatusCode());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("statusCode", response.getStatusCode().value());
            result.put("response", response.getBody());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error sending push notification", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * Create a single push notification message
     */
    private Map<String, Object> createMessage(
            String token,
            String title,
            String body,
            Map<String, Object> data,
            String channelId) {
        
        Map<String, Object> message = new HashMap<>();
        message.put("to", token);
        message.put("sound", "default");
        message.put("title", title);
        message.put("body", body);
        message.put("data", data != null ? data : new HashMap<>());
        message.put("priority", "high");
        message.put("channelId", channelId != null ? channelId : "default");
        
        return message;
    }
    
    /**
     * Send a single push notification
     */
    public Map<String, Object> sendSingleNotification(
            String token,
            String title,
            String body,
            Map<String, Object> data) {
        
        return sendPushNotifications(List.of(token), title, body, data, "default");
    }
}
