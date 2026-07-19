package com.moviebooking.ticket_booking.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, AuthUserSummary user) {
}
