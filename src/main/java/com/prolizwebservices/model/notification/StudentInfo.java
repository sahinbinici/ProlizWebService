package com.prolizwebservices.model.notification;

import lombok.Data;

/**
 * Student information for notification system
 */
@Data
public class StudentInfo {
    private String studentId;
    private String studentNo;
    private String name;
    private String surname;
    private String classId;
    private boolean hasToken;
}
