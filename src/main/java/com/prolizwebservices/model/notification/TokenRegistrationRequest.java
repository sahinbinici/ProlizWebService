package com.prolizwebservices.model.notification;

import lombok.Data;

/**
 * Request model for token registration
 */
@Data
public class TokenRegistrationRequest {
    private String token;
    private String userId;
    private String userType;
    private String platform;
    private String deviceId;
    private String osVersion;
}
