package com.moviebooking.ticket_booking.payment.handler;

import com.moviebooking.ticket_booking.payment.dto.CreatePaymentRequest;
import com.moviebooking.ticket_booking.payment.dto.PaymentResponse;
import com.moviebooking.ticket_booking.payment.mapper.PaymentMapper;
import com.moviebooking.ticket_booking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRequestHandler {

    private final PaymentService paymentService;

    public PaymentResponse pay(Long userId, CreatePaymentRequest request) {
        return PaymentMapper.toResponse(
                paymentService.pay(userId, request.bookingId(), request.method(), request.idempotencyKey()));
    }

    public PaymentResponse get(Long userId, Long paymentId) {
        return PaymentMapper.toResponse(paymentService.getPayment(userId, paymentId));
    }
}
