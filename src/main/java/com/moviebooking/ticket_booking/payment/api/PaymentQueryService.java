package com.moviebooking.ticket_booking.payment.api;

import com.moviebooking.ticket_booking.payment.entity.Payment;
import com.moviebooking.ticket_booking.payment.entity.PaymentStatus;
import com.moviebooking.ticket_booking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Published facade: lets the refund module find the payment a refund is issued against. */
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Optional<Long> findSuccessfulPaymentId(Long bookingId) {
        return paymentRepository.findFirstByBookingIdAndStatusOrderByIdDesc(bookingId, PaymentStatus.SUCCESS)
                .map(Payment::getId);
    }
}
