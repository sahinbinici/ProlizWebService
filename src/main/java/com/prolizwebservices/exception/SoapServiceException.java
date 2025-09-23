package com.prolizwebservices.exception;

/**
 * Custom exception for SOAP service related errors
 */
public class SoapServiceException extends RuntimeException {

    private final String errorCode;
    private final String soapAction;

    public SoapServiceException(String message) {
        super(message);
        this.errorCode = null;
        this.soapAction = null;
    }

    public SoapServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.soapAction = null;
    }

    public SoapServiceException(String message, String errorCode, String soapAction) {
        super(message);
        this.errorCode = errorCode;
        this.soapAction = soapAction;
    }

    public SoapServiceException(String message, Throwable cause, String errorCode, String soapAction) {
        super(message, cause);
        this.errorCode = errorCode;
        this.soapAction = soapAction;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getSoapAction() {
        return soapAction;
    }
}