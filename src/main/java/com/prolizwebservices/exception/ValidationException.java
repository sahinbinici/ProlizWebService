package com.prolizwebservices.exception;

/**
 * Custom exception for input validation errors
 */
public class ValidationException extends RuntimeException {

    private final String fieldName;
    private final Object fieldValue;

    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.fieldValue = null;
    }

    public ValidationException(String message, String fieldName, Object fieldValue) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}