package com.orang.shared.exception;

import org.springframework.http.HttpStatus;

public class EmailServiceException extends BaseException {

    private final EmailErrorType errorType;

    public enum EmailErrorType {
        CONNECTION_FAILED("Mail server connection failed"),
        AUTHENTICATION_FAILED("Mail server authentication failed"),
        TIMEOUT("Mail server request timed out"),
        TEMPLATE_ERROR("Email template processing failed"),
        INVALID_EMAIL("Invalid email address"),
        UNKNOWN("Failed to send email");

        public final String description;

        EmailErrorType(String description) {
            this.description = description;
        }
    }

    public EmailServiceException(String message, EmailErrorType errorType) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
        this.errorType = errorType;
    }

    public EmailServiceException(String message, EmailErrorType errorType, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
        this.errorType = errorType;
        this.initCause(cause);
    }

    public EmailErrorType getErrorType() {
        return errorType;
    }
}
