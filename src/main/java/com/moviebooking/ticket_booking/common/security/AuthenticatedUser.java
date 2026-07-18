package com.moviebooking.ticket_booking.common.security;

/**
 * Authenticated principal placed in the security context by {@link JwtAuthenticationFilter}.
 * Role is kept as a plain string so {@code common} stays independent of the {@code auth} module.
 */
public record AuthenticatedUser(Long userId, String email, String role) {
}
