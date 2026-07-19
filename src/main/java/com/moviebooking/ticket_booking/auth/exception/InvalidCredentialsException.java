package com.moviebooking.ticket_booking.auth.exception;

import com.moviebooking.ticket_booking.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Login failed. Deliberately does not reveal whether the email or password was wrong. Maps to HTTP 401. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
