package com.moviebooking.ticket_booking.payment.service;

import com.moviebooking.ticket_booking.booking.api.BookingConfirmationService;
import com.moviebooking.ticket_booking.payment.entity.Payment;
import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import com.moviebooking.ticket_booking.payment.entity.PaymentStatus;
import com.moviebooking.ticket_booking.payment.gateway.GatewayOutcome;
import com.moviebooking.ticket_booking.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingConfirmationService bookingConfirmationService;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    @Test
    void success_marksSuccessAndConfirmsBooking() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentProcessor.finalizePayment(1L, 2L, PaymentMethod.UPI, 20000, "key1",
                new GatewayOutcome(true, "TXN1"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTxnRef()).isEqualTo("TXN1");
        verify(bookingConfirmationService).confirm(2L);
    }

    @Test
    void failure_marksFailedAndDoesNotConfirm() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentProcessor.finalizePayment(1L, 2L, PaymentMethod.CARD, 20000, "key2",
                new GatewayOutcome(false, "TXN2"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(bookingConfirmationService, never()).confirm(any());
    }
}
