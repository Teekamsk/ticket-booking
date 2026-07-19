package com.moviebooking.ticket_booking.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and verifies stateless HS256 access tokens. */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration();
    }

    public String generateToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Verifies signature/expiry and extracts the principal. Throws {@code JwtException} if invalid. */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        long userId = ((Number) claims.get(CLAIM_USER_ID)).longValue();
        return new AuthenticatedUser(userId, claims.getSubject(), claims.get(CLAIM_ROLE, String.class));
    }

    public long getExpirySeconds() {
        return expiration.toSeconds();
    }
}
