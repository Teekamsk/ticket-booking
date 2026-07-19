package com.moviebooking.ticket_booking.payment.service;

import com.moviebooking.ticket_booking.booking.api.BookingConfirmationService;
import com.moviebooking.ticket_booking.payment.entity.Payment;
import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import com.moviebooking.ticket_booking.payment.entity.PaymentStatus;
import com.moviebooking.ticket_booking.payment.gateway.GatewayOutcome;
import com.moviebooking.ticket_booking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the payment result and, on success, confirms the booking — atomically in one transaction.
 * Kept separate from {@code PaymentService} so the (non-transactional) gateway delay stays outside it.
 */
@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private final PaymentRepository paymentRepository;
    private final BookingConfirmationService bookingConfirmationService;

    @Transactional
    public Payment finalizePayment(Long userId, Long bookingId, PaymentMethod method, long amount,
                                   String idempotencyKey, GatewayOutcome outcome) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setTxnRef(outcome.txnRef());
        if (outcome.success()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            bookingConfirmationService.confirm(bookingId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        return paymentRepository.save(payment);
    }
}
