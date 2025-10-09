package com.prolizwebservices.model.notification;

import lombok.Data;
import java.util.List;

/**
 * Lesson information for notification system
 */
@Data
public class LessonInfo {
    private String lessonId;
    private String lessonCode;
    private String lessonName;
    private String academicId;
    private int studentCount;
    private List<String> classes;
}
