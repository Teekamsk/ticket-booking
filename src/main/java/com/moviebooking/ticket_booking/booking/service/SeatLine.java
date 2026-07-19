package com.moviebooking.ticket_booking.booking.service;

/** A seat line for hold/booking responses (from held ShowSeats or from confirmed tickets). */
public record SeatLine(Long seatId, String rowLabel, int seatNumber, String seatType, long price, String status) {
}
