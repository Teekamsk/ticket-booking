package com.moviebooking.ticket_booking.auth.dto;

public record CurrentUserResponse(Long userId, String email, String role) {
}
