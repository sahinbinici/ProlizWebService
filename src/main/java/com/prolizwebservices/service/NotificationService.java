package com.prolizwebservices.service;

import com.prolizwebservices.entity.NotificationHistory;
import com.prolizwebservices.entity.NotificationToken;
import com.prolizwebservices.model.Ders;
import com.prolizwebservices.model.Ogrenci;
import com.prolizwebservices.model.OgretimElemani;
import com.prolizwebservices.model.notification.*;
import com.prolizwebservices.repository.NotificationHistoryRepository;
import com.prolizwebservices.repository.NotificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing push notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationTokenRepository tokenRepository;
    private final NotificationHistoryRepository historyRepository;
    private final ExpoPushService expoPushService;
    private final DataCacheService cacheService;
    
    // 🚀 PERFORMANCE: In-memory cache for frequently accessed lesson students
    // Cache expires after 5 minutes to prevent stale data
    private final Map<String, CachedStudentList> studentListCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    
    private static class CachedStudentList {
        final List<StudentInfo> students;
        final long timestamp;
        
        CachedStudentList(List<StudentInfo> students) {
            this.students = students;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * Register a device token
     */
    @Transactional
    public void registerToken(TokenRegistrationRequest request) {
        Optional<NotificationToken> existing = tokenRepository.findByToken(request.getToken());
        
        NotificationToken token;
        if (existing.isPresent()) {
            token = existing.get();
            log.info("Updating existing token for user: {}", request.getUserId());
        } else {
            token = new NotificationToken();
            token.setToken(request.getToken());
            log.info("Registering new token for user: {}", request.getUserId());
        }
        
        token.setUserId(request.getUserId());
        token.setUserType(NotificationToken.UserType.valueOf(request.getUserType().toUpperCase()));
        token.setPlatform(request.getPlatform());
        token.setDeviceId(request.getDeviceId());
        token.setOsVersion(request.getOsVersion());
        
        tokenRepository.save(token);
        log.info("Token registered successfully for user: {}", request.getUserId());
    }
    
    /**
     * Unregister a device token
     */
    @Transactional
    public void unregisterToken(String token) {
        tokenRepository.deleteByToken(token);
        log.info("Token unregistered: {}", token);
    }
    
    /**
     * Get academic's lessons from CACHE (not from database)
     * Uses DataCacheService which loads data from SOAP web service
     */
    public List<LessonInfo> getAcademicLessons(String academicId) {
        if (!cacheService.isInitialized()) {
            log.warn("Cache not initialized yet - data is loading from SOAP service");
            return Collections.emptyList();
        }
        
        // Find academic by sicil number from CACHE
        OgretimElemani ogretimElemani = cacheService.getOgretimElemaniBySicil(academicId);
        if (ogretimElemani == null) {
            log.warn("Academic not found in cache: {}", academicId);
            return Collections.emptyList();
        }
        
        String tcKimlikNo = ogretimElemani.getTcKimlikNo();
        log.info("Found academic: {} {} (TC: {})", ogretimElemani.getAdi(), ogretimElemani.getSoyadi(), tcKimlikNo);
        
        // Get all courses from CACHE
        List<Ders> allDersler = cacheService.getAllDersler();
        
        // Filter courses taught by this academic
        List<Ders> academicDersler = allDersler.stream()
            .filter(ders -> tcKimlikNo.equals(ders.getOgretimElemaniTC()))
            .collect(Collectors.toList());
        
        log.info("Found {} courses for academic {}", academicDersler.size(), academicId);
        
        // Convert to LessonInfo with student data from CACHE
        return academicDersler.stream()
            .map(ders -> {
                LessonInfo info = new LessonInfo();
                info.setLessonId(ders.getDersHarId());
                info.setLessonCode(ders.getDersKodu());
                info.setLessonName(ders.getDersAdi());
                info.setAcademicId(academicId);
                
                // Get students from CACHE (not database)
                List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId(ders.getDersHarId());
                info.setStudentCount(ogrenciler.size());
                
                // Get unique classes from student data
                Set<String> classes = ogrenciler.stream()
                    .map(Ogrenci::getSinif)
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .collect(Collectors.toSet());
                info.setClasses(new ArrayList<>(classes));
                
                return info;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get students for a lesson from CACHE (not from database)
     * Student data comes from SOAP web service via DataCacheService
     * OPTIMIZED: Single DB query for all tokens instead of N queries
     * ULTRA-OPTIMIZED: Parallel processing for large student lists
     * CACHED: Results cached for 5 minutes to reduce repeated queries
     */
    public List<StudentInfo> getLessonStudents(String lessonId) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (!cacheService.isInitialized()) {
                log.warn("Cache not initialized - SOAP data still loading");
                return Collections.emptyList();
            }
            
            // 🚀 Check in-memory cache first
            CachedStudentList cached = studentListCache.get(lessonId);
            if (cached != null && !cached.isExpired()) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("⚡ Cache HIT for lesson {} - returned {} students in {}ms", 
                    lessonId, cached.students.size(), duration);
                return cached.students;
            }
            
            // Get students from CACHE (loaded from SOAP service)
            List<Ogrenci> ogrenciler = cacheService.getOgrencilerByDersHarId(lessonId);
            log.info("Found {} students for lesson {} from cache", ogrenciler.size(), lessonId);
            
            if (ogrenciler.isEmpty()) {
                log.info("No students found for lesson {}", lessonId);
                return Collections.emptyList();
            }
            
            // OPTIMIZATION: Get all student IDs first
            List<String> studentIds = ogrenciler.stream()
                .map(Ogrenci::getOgrNo)
                .collect(Collectors.toList());
            
            // OPTIMIZATION: Single DB query to get all tokens at once
            List<NotificationToken> tokens = tokenRepository.findByUserIdInAndUserType(
                studentIds, 
                NotificationToken.UserType.STUDENT
            );
            
            // Create a Set for fast lookup
            Set<String> studentIdsWithTokens = tokens.stream()
                .map(NotificationToken::getUserId)
                .collect(Collectors.toSet());
            
            log.info("Found {} students with notification tokens", studentIdsWithTokens.size());
            
            // ULTRA-OPTIMIZATION: Use parallel stream for large lists (>100 students)
            List<StudentInfo> result = (ogrenciler.size() > 100 ? ogrenciler.parallelStream() : ogrenciler.stream())
                .map(ogrenci -> {
                    StudentInfo info = new StudentInfo();
                    info.setStudentId(ogrenci.getOgrNo());
                    info.setStudentNo(ogrenci.getOgrNo());
                    info.setName(ogrenci.getAdi());
                    info.setSurname(ogrenci.getSoyadi());
                    info.setClassId(ogrenci.getSinif());
                    
                    // Fast lookup: O(1) instead of DB query
                    info.setHasToken(studentIdsWithTokens.contains(ogrenci.getOgrNo()));
                    
                    return info;
                })
                .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ getLessonStudents completed in {}ms for {} students", duration, result.size());
            
            // 🚀 Store in cache for future requests
            studentListCache.put(lessonId, new CachedStudentList(result));
            
            // Clean up expired cache entries (simple cleanup)
            if (studentListCache.size() > 1000) {
                studentListCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Error in getLessonStudents for lesson {} after {}ms: {}", 
                lessonId, duration, e.getMessage(), e);
            throw new RuntimeException("Failed to get lesson students: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get students for a lesson and class
     */
    public List<StudentInfo> getLessonClassStudents(String lessonId, String classId) {
        List<StudentInfo> allStudents = getLessonStudents(lessonId);
        
        return allStudents.stream()
            .filter(student -> classId.equals(student.getClassId()))
            .collect(Collectors.toList());
    }
    
    /**
     * Send bulk notification to all students in a lesson
     */
    @Transactional
    public SendNotificationResponse sendBulkNotification(SendNotificationRequest request) {
        List<StudentInfo> students = getLessonStudents(request.getLessonId());
        List<String> studentIds = students.stream()
            .map(StudentInfo::getStudentId)
            .collect(Collectors.toList());
        
        return sendNotificationToStudents(
            studentIds,
            request.getTitle(),
            request.getBody(),
            request.getData(),
            request,
            NotificationHistory.RecipientType.ALL
        );
    }
    
    /**
     * Send notification to a specific class
     */
    @Transactional
    public SendNotificationResponse sendClassNotification(SendNotificationRequest request) {
        List<StudentInfo> students = getLessonClassStudents(request.getLessonId(), request.getClassId());
        List<String> studentIds = students.stream()
            .map(StudentInfo::getStudentId)
            .collect(Collectors.toList());
        
        return sendNotificationToStudents(
            studentIds,
            request.getTitle(),
            request.getBody(),
            request.getData(),
            request,
            NotificationHistory.RecipientType.CLASS
        );
    }
    
    /**
     * Send notification to individual students
     */
    @Transactional
    public SendNotificationResponse sendIndividualNotification(SendNotificationRequest request) {
        return sendNotificationToStudents(
            request.getStudentIds(),
            request.getTitle(),
            request.getBody(),
            request.getData(),
            request,
            NotificationHistory.RecipientType.INDIVIDUAL
        );
    }
    
    /**
     * Internal method to send notifications to students
     */
    private SendNotificationResponse sendNotificationToStudents(
            List<String> studentIds,
            String title,
            String body,
            Map<String, Object> data,
            SendNotificationRequest request,
            NotificationHistory.RecipientType recipientType) {
        
        if (studentIds == null || studentIds.isEmpty()) {
            log.warn("No students to send notification to");
            return new SendNotificationResponse(false, 0, 0, "No students found");
        }
        
        // Get tokens for students
        List<NotificationToken> tokens = tokenRepository.findByUserIdIn(studentIds);
        List<String> expoPushTokens = tokens.stream()
            .map(NotificationToken::getToken)
            .collect(Collectors.toList());
        
        int sentCount = 0;
        int failedCount = 0;
        
        if (!expoPushTokens.isEmpty()) {
            try {
                // Send push notifications
                Map<String, Object> result = expoPushService.sendPushNotifications(
                    expoPushTokens,
                    title,
                    body,
                    data,
                    "lessons"
                );
                
                if (Boolean.TRUE.equals(result.get("success"))) {
                    sentCount = expoPushTokens.size();
                    log.info("Successfully sent {} notifications", sentCount);
                } else {
                    failedCount = expoPushTokens.size();
                    log.error("Failed to send notifications: {}", result.get("error"));
                }
            } catch (Exception e) {
                log.error("Error sending notifications", e);
                failedCount = expoPushTokens.size();
            }
        }
        
        // Calculate students without tokens
        int studentsWithoutTokens = studentIds.size() - tokens.size();
        
        // Save to history
        saveNotificationHistory(request, recipientType, studentIds.size(), sentCount, failedCount);
        
        String message = String.format(
            "Notification sent to %d students. %d without tokens.",
            sentCount,
            studentsWithoutTokens
        );
        
        return new SendNotificationResponse(true, sentCount, failedCount, message);
    }
    
    /**
     * Save notification to history
     */
    private void saveNotificationHistory(
            SendNotificationRequest request,
            NotificationHistory.RecipientType recipientType,
            int recipientCount,
            int sentCount,
            int failedCount) {
        
        NotificationHistory history = new NotificationHistory();
        history.setAcademicId(request.getAcademicId());
        history.setLessonId(request.getLessonId());
        history.setTitle(request.getTitle());
        history.setBody(request.getBody());
        history.setRecipientType(recipientType);
        history.setClassId(request.getClassId());
        history.setRecipientCount(recipientCount);
        history.setSentCount(sentCount);
        history.setFailedCount(failedCount);
        
        historyRepository.save(history);
        log.info("Notification history saved for academic: {}", request.getAcademicId());
    }
    
    /**
     * Get notification history for an academic
     */
    public List<NotificationHistory> getNotificationHistory(String academicId, int limit) {
        List<NotificationHistory> history = historyRepository.findByAcademicIdOrderByCreatedAtDesc(academicId);
        
        if (limit > 0 && history.size() > limit) {
            return history.subList(0, limit);
        }
        
        return history;
    }
    
    /**
     * Clear student list cache (useful when tokens are updated)
     */
    public void clearStudentListCache() {
        int size = studentListCache.size();
        studentListCache.clear();
        log.info("🗑️ Cleared student list cache ({} entries)", size);
    }
    
    /**
     * Clear cache for specific lesson
     */
    public void clearLessonCache(String lessonId) {
        studentListCache.remove(lessonId);
        log.info("🗑️ Cleared cache for lesson {}", lessonId);
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCachedLessons", studentListCache.size());
        stats.put("expiredEntries", studentListCache.values().stream().filter(CachedStudentList::isExpired).count());
        return stats;
    }
}
