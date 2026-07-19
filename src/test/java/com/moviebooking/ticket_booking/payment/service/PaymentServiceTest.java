package com.moviebooking.ticket_booking.payment.service;

import com.moviebooking.ticket_booking.booking.api.BookingConfirmationService;
import com.moviebooking.ticket_booking.booking.api.PayableBooking;
import com.moviebooking.ticket_booking.payment.entity.Payment;
import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import com.moviebooking.ticket_booking.payment.gateway.GatewayOutcome;
import com.moviebooking.ticket_booking.payment.gateway.PaymentGateway;
import com.moviebooking.ticket_booking.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingConfirmationService bookingConfirmationService;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentProcessor paymentProcessor;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void pay_existingIdempotencyKey_returnsExistingWithoutCharging() {
        Payment existing = new Payment();
        when(paymentRepository.findByIdempotencyKey("dup")).thenReturn(Optional.of(existing));

        Payment result = paymentService.pay(1L, 2L, PaymentMethod.UPI, "dup");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(paymentGateway, paymentProcessor);
        verify(bookingConfirmationService, never()).loadPayable(anyLong(), anyLong());
    }

    @Test
    void pay_newKey_chargesAndFinalizes() {
        when(paymentRepository.findByIdempotencyKey("new")).thenReturn(Optional.empty());
        when(bookingConfirmationService.loadPayable(2L, 1L)).thenReturn(new PayableBooking(2L, 20000));
        GatewayOutcome outcome = new GatewayOutcome(true, "TXN");
        when(paymentGateway.charge(20000, PaymentMethod.UPI)).thenReturn(outcome);
        Payment finalized = new Payment();
        when(paymentProcessor.finalizePayment(1L, 2L, PaymentMethod.UPI, 20000, "new", outcome))
                .thenReturn(finalized);

        Payment result = paymentService.pay(1L, 2L, PaymentMethod.UPI, "new");

        assertThat(result).isSameAs(finalized);
        verify(paymentGateway).charge(20000, PaymentMethod.UPI);
        verify(paymentProcessor).finalizePayment(eq(1L), eq(2L), eq(PaymentMethod.UPI), eq(20000L), eq("new"), any());
    }
}
