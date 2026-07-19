package com.moviebooking.ticket_booking.payment.web;

import com.moviebooking.ticket_booking.common.security.AuthenticatedUser;
import com.moviebooking.ticket_booking.payment.dto.CreatePaymentRequest;
import com.moviebooking.ticket_booking.payment.dto.PaymentResponse;
import com.moviebooking.ticket_booking.payment.handler.PaymentRequestHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Simulated payments. Customer-only. */
@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRequestHandler paymentRequestHandler;

    @PostMapping
    public PaymentResponse pay(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody CreatePaymentRequest request) {
        return paymentRequestHandler.pay(user.userId(), request);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return paymentRequestHandler.get(user.userId(), id);
    }
}
