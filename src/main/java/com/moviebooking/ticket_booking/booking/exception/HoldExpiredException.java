package com.moviebooking.ticket_booking.booking.exception;

import com.moviebooking.ticket_booking.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** The seat hold has expired or is no longer active. Maps to HTTP 410 Gone. */
public class HoldExpiredException extends ApiException {

    public HoldExpiredException(String message) {
        super(HttpStatus.GONE, "HOLD_EXPIRED", message);
    }
}
