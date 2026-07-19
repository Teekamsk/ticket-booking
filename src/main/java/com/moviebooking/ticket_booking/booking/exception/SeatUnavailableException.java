package com.moviebooking.ticket_booking.booking.exception;

import com.moviebooking.ticket_booking.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** One or more requested seats are already held or booked. Maps to HTTP 409. */
public class SeatUnavailableException extends ApiException {

    public SeatUnavailableException(String message) {
        super(HttpStatus.CONFLICT, "SEAT_UNAVAILABLE", message);
    }
}
