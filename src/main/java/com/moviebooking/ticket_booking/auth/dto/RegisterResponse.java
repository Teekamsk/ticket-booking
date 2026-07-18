package com.moviebooking.ticket_booking.auth.dto;

import com.moviebooking.ticket_booking.auth.entity.Role;

public record RegisterResponse(Long id, String name, String email, Role role) {
}
