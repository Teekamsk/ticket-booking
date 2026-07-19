package com.moviebooking.ticket_booking.common.security;

import tools.jackson.databind.ObjectMapper;
import com.moviebooking.ticket_booking.common.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a 401 Problem Details when an unauthenticated request hits a protected resource. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetails.write(response, objectMapper, HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource", "UNAUTHENTICATED");
    }
}
