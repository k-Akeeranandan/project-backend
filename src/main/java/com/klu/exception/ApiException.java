package com.klu.exception;

import org.springframework.http.HttpStatus;

/**
 * Application-level errors with an explicit HTTP status (validation, conflict, auth, etc.).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
