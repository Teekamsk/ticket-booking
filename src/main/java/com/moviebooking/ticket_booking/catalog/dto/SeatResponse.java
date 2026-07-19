package com.moviebooking.ticket_booking.catalog.dto;

import com.moviebooking.ticket_booking.catalog.entity.SeatType;

public record SeatResponse(Long id, Long screenId, String rowLabel, int seatNumber, SeatType seatType, boolean active) {
}
