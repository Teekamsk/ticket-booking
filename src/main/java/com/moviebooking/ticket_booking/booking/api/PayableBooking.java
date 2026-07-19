package com.moviebooking.ticket_booking.booking.api;

/** What the payment module needs to charge for a booking. */
public record PayableBooking(Long bookingId, long payableAmount) {
}
