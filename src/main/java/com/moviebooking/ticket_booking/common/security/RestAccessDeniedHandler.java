package com.moviebooking.ticket_booking.common.security;

import tools.jackson.databind.ObjectMapper;
import com.moviebooking.ticket_booking.common.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a 403 Problem Details when an authenticated user lacks the required role. */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetails.write(response, objectMapper, HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource", "ACCESS_DENIED");
    }
}
