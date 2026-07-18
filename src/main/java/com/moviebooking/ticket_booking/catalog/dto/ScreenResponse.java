package com.moviebooking.ticket_booking.catalog.dto;

public record ScreenResponse(Long id, Long theatreId, String name, int totalSeats, boolean active) {
}
