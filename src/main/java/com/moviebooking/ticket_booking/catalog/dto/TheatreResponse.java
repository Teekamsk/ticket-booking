package com.moviebooking.ticket_booking.catalog.dto;

public record TheatreResponse(Long id, Long cityId, String name, String address, boolean active) {
}
