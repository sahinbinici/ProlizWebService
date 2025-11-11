package com.prolizwebservices.controller;

import com.prolizwebservices.entity.NotificationHistory;
import com.prolizwebservices.model.notification.*;
import com.prolizwebservices.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Push Notification Management
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(
    origins = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, 
    allowedHeaders = "*",
    allowCredentials = "false",
    maxAge = 3600
)
@Tag(name = "D-Push Notifications", description = "Push notification management for academic staff to send notifications to students")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * Register a device token
     */
    @Operation(
        summary = "Register Device Token",
        description = "Registers a device token for push notifications. Called when user logs in."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/register-token")
    public ResponseEntity<Map<String, Object>> registerToken(
            @RequestBody TokenRegistrationRequest request) {
        
        try {
            notificationService.registerToken(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token registered successfully");
            
            log.info("Token registered for user: {} ({})", request.getUserId(), request.getUserType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error registering token", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to register token: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Unregister a device token
     */
    @Operation(
        summary = "Unregister Device Token",
        description = "Removes a device token. Called when user logs out."
    )
    @PostMapping("/unregister-token")
    public ResponseEntity<Map<String, Object>> unregisterToken(
            @RequestBody Map<String, String> request) {
        
        try {
            String token = request.get("token");
            notificationService.unregisterToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token unregistered successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error unregistering token", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to unregister token: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get academic's lessons
     */
    @Operation(
        summary = "Get Academic's Lessons",
        description = "Returns list of lessons taught by an academic staff member"
    )
    @GetMapping("/academic/{academicId}/lessons")
    public ResponseEntity<List<LessonInfo>> getAcademicLessons(
            @Parameter(description = "Academic registry number (Sicil No)", required = true, example = "12345")
            @PathVariable String academicId) {
        
        List<LessonInfo> lessons = notificationService.getAcademicLessons(academicId);
        return ResponseEntity.ok(lessons);
    }
    
    /**
     * Get students for a lesson
     */
    @Operation(
        summary = "Get Lesson Students",
        description = "Returns all students enrolled in a specific lesson with notification token status"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/lesson/{lessonId}/students")
    public ResponseEntity<?> getLessonStudents(
            @Parameter(description = "Lesson ID (DersHarID)", required = true, example = "2838793")
            @PathVariable String lessonId) {
        
        try {
            long startTime = System.currentTimeMillis();
            log.info("📥 Getting students for lesson: {}", lessonId);
            
            List<StudentInfo> students = notificationService.getLessonStudents(lessonId);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Retrieved {} students in {}ms", students.size(), duration);
            
            // Add metadata to response
            Map<String, Object> response = new HashMap<>();
            response.put("students", students);
            response.put("totalStudents", students.size());
            response.put("studentsWithTokens", students.stream().filter(StudentInfo::isHasToken).count());
            response.put("responseTimeMs", duration);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting students for lesson {}: {}", lessonId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve students");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("lessonId", lessonId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get students for a lesson and class
     */
    @Operation(
        summary = "Get Lesson Class Students",
        description = "Returns students enrolled in a specific lesson and class"
    )
    @GetMapping("/lesson/{lessonId}/class/{classId}/students")
    public ResponseEntity<List<StudentInfo>> getLessonClassStudents(
            @Parameter(description = "Lesson ID (DersHarID)", required = true, example = "2838793")
            @PathVariable String lessonId,
            @Parameter(description = "Class ID", required = true, example = "1A")
            @PathVariable String classId) {
        
        List<StudentInfo> students = notificationService.getLessonClassStudents(lessonId, classId);
        return ResponseEntity.ok(students);
    }
    
    /**
     * Send bulk notification to all students in a lesson
     */
    @Operation(
        summary = "Send Bulk Notification",
        description = "Sends notification to all students enrolled in a lesson"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/send-bulk")
    public ResponseEntity<SendNotificationResponse> sendBulkNotification(
            @RequestBody SendNotificationRequest request) {
        
        try {
            log.info("Sending bulk notification for lesson: {} by academic: {}", 
                request.getLessonId(), request.getAcademicId());
            
            SendNotificationResponse response = notificationService.sendBulkNotification(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending bulk notification", e);
            return ResponseEntity.badRequest()
                .body(new SendNotificationResponse(false, 0, 0, "Error: " + e.getMessage()));
        }
    }
    
    /**
     * Send notification to a specific class
     */
    @Operation(
        summary = "Send Class Notification",
        description = "Sends notification to students in a specific class"
    )
    @PostMapping("/send-class")
    public ResponseEntity<SendNotificationResponse> sendClassNotification(
            @RequestBody SendNotificationRequest request) {
        
        try {
            log.info("Sending class notification for lesson: {}, class: {} by academic: {}", 
                request.getLessonId(), request.getClassId(), request.getAcademicId());
            
            SendNotificationResponse response = notificationService.sendClassNotification(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending class notification", e);
            return ResponseEntity.badRequest()
                .body(new SendNotificationResponse(false, 0, 0, "Error: " + e.getMessage()));
        }
    }
    
    /**
     * Send notification to individual students
     */
    @Operation(
        summary = "Send Individual Notification",
        description = "Sends notification to selected individual students"
    )
    @PostMapping("/send-individual")
    public ResponseEntity<SendNotificationResponse> sendIndividualNotification(
            @RequestBody SendNotificationRequest request) {
        
        try {
            log.info("Sending individual notification to {} students by academic: {}", 
                request.getStudentIds() != null ? request.getStudentIds().size() : 0, 
                request.getAcademicId());
            
            SendNotificationResponse response = notificationService.sendIndividualNotification(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending individual notification", e);
            return ResponseEntity.badRequest()
                .body(new SendNotificationResponse(false, 0, 0, "Error: " + e.getMessage()));
        }
    }
    
    /**
     * Get notification history for an academic
     */
    @Operation(
        summary = "Get Notification History",
        description = "Returns notification history for an academic staff member"
    )
    @GetMapping("/history/{academicId}")
    public ResponseEntity<List<NotificationHistory>> getNotificationHistory(
            @Parameter(description = "Academic registry number (Sicil No)", required = true, example = "12345")
            @PathVariable String academicId,
            @Parameter(description = "Maximum number of records to return", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        
        List<NotificationHistory> history = notificationService.getNotificationHistory(academicId, limit);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Clear student list cache
     */
    @Operation(
        summary = "Clear Student List Cache",
        description = "Clears the in-memory cache for student lists (useful after token updates)"
    )
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            notificationService.clearStudentListCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student list cache cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to clear cache: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get cache statistics
     */
    @Operation(
        summary = "Get Cache Statistics",
        description = "Returns statistics about the student list cache"
    )
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = notificationService.getCacheStats();
        return ResponseEntity.ok(stats);
    }
}
