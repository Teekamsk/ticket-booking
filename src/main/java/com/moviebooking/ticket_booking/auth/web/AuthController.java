package com.moviebooking.ticket_booking.auth.web;

import com.moviebooking.ticket_booking.auth.dto.CurrentUserResponse;
import com.moviebooking.ticket_booking.auth.dto.LoginRequest;
import com.moviebooking.ticket_booking.auth.dto.LoginResponse;
import com.moviebooking.ticket_booking.auth.dto.RegisterRequest;
import com.moviebooking.ticket_booking.auth.dto.RegisterResponse;
import com.moviebooking.ticket_booking.auth.handler.AuthRequestHandler;
import com.moviebooking.ticket_booking.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthRequestHandler authRequestHandler;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authRequestHandler.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authRequestHandler.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authRequestHandler.currentUser(principal);
    }
}
