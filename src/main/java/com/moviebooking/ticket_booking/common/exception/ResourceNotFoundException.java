package com.moviebooking.ticket_booking.common.exception;

import org.springframework.http.HttpStatus;

/** Requested resource does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
