package com.moviebooking.ticket_booking.discount.api;

/** Result of validating + computing a discount, consumed by the booking module. */
public record DiscountResult(Long discountId, String code, long discountAmount) {
}
