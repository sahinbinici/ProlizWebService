package com.prolizwebservices.model.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for notification sending
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendNotificationResponse {
    private boolean success;
    private int sentCount;
    private int failedCount;
    private String message;
}
