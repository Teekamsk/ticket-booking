package com.moviebooking.ticket_booking.refund.service;

import com.moviebooking.ticket_booking.booking.event.BookingCancelledEvent;
import com.moviebooking.ticket_booking.payment.api.PaymentQueryService;
import com.moviebooking.ticket_booking.refund.entity.Refund;
import com.moviebooking.ticket_booking.refund.entity.RefundStatus;
import com.moviebooking.ticket_booking.refund.event.RefundProcessedEvent;
import com.moviebooking.ticket_booking.refund.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private PaymentQueryService paymentQueryService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RefundService refundService;

    private BookingCancelledEvent event(int percent) {
        return new BookingCancelledEvent(2L, "BK123", 7L, 9L, percent, 40000);
    }

    @Test
    void zeroPercent_noRefundCreated() {
        refundService.processRefund(event(0));
        verify(refundRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void alreadyRefunded_skips() {
        when(refundRepository.existsByBookingId(2L)).thenReturn(true);
        refundService.processRefund(event(50));
        verify(refundRepository, never()).save(any());
    }

    @Test
    void noSuccessfulPayment_skips() {
        when(refundRepository.existsByBookingId(2L)).thenReturn(false);
        when(paymentQueryService.findSuccessfulPaymentId(2L)).thenReturn(Optional.empty());
        refundService.processRefund(event(50));
        verify(refundRepository, never()).save(any());
    }

    @Test
    void eligible_createsProcessedRefundAndPublishes() {
        when(refundRepository.existsByBookingId(2L)).thenReturn(false);
        when(paymentQueryService.findSuccessfulPaymentId(2L)).thenReturn(Optional.of(11L));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        refundService.processRefund(event(50));

        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(captor.capture());
        Refund refund = captor.getValue();
        assertThat(refund.getAmount()).isEqualTo(20000); // 50% of 40000
        assertThat(refund.getPaymentId()).isEqualTo(11L);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSED);
        verify(eventPublisher).publishEvent(any(RefundProcessedEvent.class));
    }
}
