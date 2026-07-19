package com.moviebooking.ticket_booking.auth.handler;

import com.moviebooking.ticket_booking.auth.dto.AuthUserSummary;
import com.moviebooking.ticket_booking.auth.dto.CurrentUserResponse;
import com.moviebooking.ticket_booking.auth.dto.LoginRequest;
import com.moviebooking.ticket_booking.auth.dto.LoginResponse;
import com.moviebooking.ticket_booking.auth.dto.RegisterRequest;
import com.moviebooking.ticket_booking.auth.dto.RegisterResponse;
import com.moviebooking.ticket_booking.auth.entity.User;
import com.moviebooking.ticket_booking.auth.service.AuthService;
import com.moviebooking.ticket_booking.common.security.AuthenticatedUser;
import com.moviebooking.ticket_booking.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Orchestrates auth requests: calls the service, issues tokens, maps entity ↔ DTO. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRequestHandler {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthService authService;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {
        User user = authService.register(request.name(), request.email(), request.phone(), request.password());
        return new RegisterResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public LoginResponse login(LoginRequest request) {
        User user = authService.authenticate(request.email(), request.password());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        log.info("Issued token for {} (id={})", user.getEmail(), user.getId());
        return new LoginResponse(token, TOKEN_TYPE, jwtService.getExpirySeconds(),
                new AuthUserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }

    public CurrentUserResponse currentUser(AuthenticatedUser principal) {
        return new CurrentUserResponse(principal.userId(), principal.email(), principal.role());
    }
}
