package com.moviebooking.ticket_booking.catalog.dto;

import java.util.List;

public record CreateSeatsResponse(Long screenId, int createdCount, int totalSeats, List<SeatResponse> seats) {
}
