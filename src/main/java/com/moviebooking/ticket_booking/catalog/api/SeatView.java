package com.moviebooking.ticket_booking.catalog.api;

/** Published cross-module view of a physical seat. {@code seatType} is the enum name as a string. */
public record SeatView(Long seatId, String rowLabel, int seatNumber, String seatType) {
}
