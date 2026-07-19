package com.moviebooking.ticket_booking.auth.dto;

import com.moviebooking.ticket_booking.auth.entity.Role;

public record AuthUserSummary(Long id, String name, String email, Role role) {
}
