package com.moviebooking.ticket_booking.discount.exception;

import com.moviebooking.ticket_booking.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** A discount code cannot be applied (invalid, expired, ineligible, or used up). Maps to HTTP 422. */
public class DiscountNotApplicableException extends ApiException {

    public DiscountNotApplicableException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "DISCOUNT_NOT_APPLICABLE", message);
    }
}
