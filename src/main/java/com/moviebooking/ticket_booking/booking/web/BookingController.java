package com.moviebooking.ticket_booking.booking.web;

import com.moviebooking.ticket_booking.booking.dto.BookingHistoryResponse;
import com.moviebooking.ticket_booking.booking.dto.BookingResponse;
import com.moviebooking.ticket_booking.booking.dto.CancelBookingRequest;
import com.moviebooking.ticket_booking.booking.dto.CreateBookingRequest;
import com.moviebooking.ticket_booking.booking.handler.BookingRequestHandler;
import com.moviebooking.ticket_booking.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bookings. Customer-only. */
@RestController
@RequestMapping("/api/v1/bookings")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRequestHandler bookingRequestHandler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingRequestHandler.create(user.userId(), request);
    }

    @GetMapping
    public BookingHistoryResponse list(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return bookingRequestHandler.list(user.userId(), page, size);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return bookingRequestHandler.get(user.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                  @RequestBody(required = false) CancelBookingRequest request) {
        return bookingRequestHandler.cancel(user.userId(), id, request == null ? null : request.reason());
    }
}
