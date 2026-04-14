package com.orang.shared.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends BaseException{
    public TooManyRequestsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
