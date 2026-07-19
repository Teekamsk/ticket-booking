package com.moviebooking.ticket_booking.common.web;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.time.Instant;

/** Builds RFC-7807 Problem Details with a consistent {@code errorCode} + {@code timestamp}. */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String detail, String errorCode) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /** Writes a Problem Details body directly to the response — used by the security filter chain. */
    public static void write(HttpServletResponse response, ObjectMapper mapper,
                             HttpStatus status, String detail, String errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), of(status, detail, errorCode));
    }
}
