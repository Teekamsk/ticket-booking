package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.SeatType;

/** Service-level command for one row of seats to generate (keeps the service web-DTO agnostic). */
public record SeatRowSpec(String rowLabel, SeatType seatType, int count) {
}
