package com.moviebooking.ticket_booking.common.web;

import com.moviebooking.ticket_booking.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** Maps exceptions to RFC-7807 Problem Details. Any {@link ApiException} maps by its own status/code. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        log.warn("API error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ProblemDetails.of(ex.getStatus(), ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage()))
                .toList();
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_ERROR");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /** Method-security (@PreAuthorize) denials surface here at the MVC layer, not the filter chain. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource", "ACCESS_DENIED");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "INTERNAL_ERROR");
    }
}
