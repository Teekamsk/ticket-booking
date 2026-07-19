package com.moviebooking.ticket_booking.payment.mapper;

import com.moviebooking.ticket_booking.payment.dto.PaymentResponse;
import com.moviebooking.ticket_booking.payment.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBookingId(), payment.getAmount(),
                payment.getMethod(), payment.getStatus(), payment.getTxnRef());
    }
}
