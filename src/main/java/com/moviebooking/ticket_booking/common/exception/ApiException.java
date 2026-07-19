package com.moviebooking.ticket_booking.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base for all domain exceptions. Carries the HTTP status and a stable machine-readable
 * error code so {@code GlobalExceptionHandler} can map any subclass without modification.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
