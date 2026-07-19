package com.moviebooking.ticket_booking.refund.web;

import com.moviebooking.ticket_booking.common.security.AuthenticatedUser;
import com.moviebooking.ticket_booking.refund.dto.RefundResponse;
import com.moviebooking.ticket_booking.refund.handler.RefundRequestHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer view of their refund for a booking. */
@RestController
@RequestMapping("/api/v1/refunds")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class RefundController {

    private final RefundRequestHandler refundRequestHandler;

    @GetMapping
    public RefundResponse getByBooking(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestParam Long bookingId) {
        return refundRequestHandler.getForBooking(user.userId(), bookingId);
    }
}
