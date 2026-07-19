package com.moviebooking.ticket_booking.payment.service;

import com.moviebooking.ticket_booking.booking.api.BookingConfirmationService;
import com.moviebooking.ticket_booking.booking.api.PayableBooking;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.payment.entity.Payment;
import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import com.moviebooking.ticket_booking.payment.gateway.GatewayOutcome;
import com.moviebooking.ticket_booking.payment.gateway.PaymentGateway;
import com.moviebooking.ticket_booking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Orchestrates a payment: idempotency replay, load the payable booking, run the (delayed) simulated
 * gateway outside any transaction, then finalize (persist + confirm) atomically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingConfirmationService bookingConfirmationService;
    private final PaymentGateway paymentGateway;
    private final PaymentProcessor paymentProcessor;

    public Payment pay(Long userId, Long bookingId, PaymentMethod method, String idempotencyKey) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {} -> payment {}", idempotencyKey, existing.get().getId());
            return existing.get();
        }
        PayableBooking payable = bookingConfirmationService.loadPayable(bookingId, userId);
        GatewayOutcome outcome = paymentGateway.charge(payable.payableAmount(), method);
        return paymentProcessor.finalizePayment(userId, bookingId, method, payable.payableAmount(),
                idempotencyKey, outcome);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Payment " + paymentId + " not found");
        }
        return payment;
    }
}
