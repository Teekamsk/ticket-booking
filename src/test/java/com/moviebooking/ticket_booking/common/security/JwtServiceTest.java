package com.moviebooking.ticket_booking.common.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-please-use-32-bytes-minimum-abcdef";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));

    @Test
    void generateThenParse_roundTripsClaims() {
        String token = jwtService.generateToken(42L, "jane@example.com", "CUSTOMER");

        AuthenticatedUser user = jwtService.parse(token);

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.email()).isEqualTo("jane@example.com");
        assertThat(user.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void parse_tamperedToken_throws() {
        String token = jwtService.generateToken(1L, "a@b.com", "ADMIN");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(Exception.class);
    }

    @Test
    void parse_expiredToken_throws() {
        JwtService expiring = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-10)));
        String token = expiring.generateToken(1L, "a@b.com", "ADMIN");

        assertThatThrownBy(() -> expiring.parse(token)).isInstanceOf(Exception.class);
    }
}
