package com.prolizwebservices.model.notification;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Request model for sending notifications
 */
@Data
public class SendNotificationRequest {
    private String title;
    private String body;
    private Map<String, Object> data;
    private String recipientType; // all, class, individual
    private String lessonId;
    private String classId;
    private List<String> studentIds;
    private String academicId;
}
