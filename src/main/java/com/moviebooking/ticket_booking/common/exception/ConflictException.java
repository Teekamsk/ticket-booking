package com.moviebooking.ticket_booking.common.exception;

import org.springframework.http.HttpStatus;

/** Request conflicts with current state (e.g. duplicate unique value). Maps to HTTP 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
