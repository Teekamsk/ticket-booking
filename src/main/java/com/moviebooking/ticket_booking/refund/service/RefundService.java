package com.moviebooking.ticket_booking.refund.service;

import com.moviebooking.ticket_booking.booking.event.BookingCancelledEvent;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.payment.api.PaymentQueryService;
import com.moviebooking.ticket_booking.refund.entity.Refund;
import com.moviebooking.ticket_booking.refund.entity.RefundStatus;
import com.moviebooking.ticket_booking.refund.event.RefundProcessedEvent;
import com.moviebooking.ticket_booking.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Creates and (simulated-)processes a refund against the original payment on cancellation. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentQueryService paymentQueryService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processRefund(BookingCancelledEvent event) {
        if (event.refundPercentApplied() <= 0) {
            log.info("Booking {} cancelled at 0% — no payout", event.bookingId());
            return;
        }
        if (refundRepository.existsByBookingId(event.bookingId())) {
            return; // idempotent
        }
        Optional<Long> paymentId = paymentQueryService.findSuccessfulPaymentId(event.bookingId());
        if (paymentId.isEmpty()) {
            log.warn("No successful payment for booking {}; cannot refund", event.bookingId());
            return;
        }
        long amount = event.paidAmount() * event.refundPercentApplied() / 100;
        Refund refund = new Refund();
        refund.setCancellationId(event.cancellationId());
        refund.setPaymentId(paymentId.get());
        refund.setBookingId(event.bookingId());
        refund.setUserId(event.userId());
        refund.setAmount(amount);
        refund.setStatus(RefundStatus.PROCESSED); // simulated instant settlement
        refund.setProcessedAt(Instant.now());
        refundRepository.save(refund);

        eventPublisher.publishEvent(new RefundProcessedEvent(event.userId(), event.bookingId(),
                event.bookingRef(), amount));
        log.info("Refund of {} paise processed for booking {}", amount, event.bookingId());
    }

    @Transactional(readOnly = true)
    public Refund getForBooking(Long userId, Long bookingId) {
        Refund refund = refundRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No refund found for booking " + bookingId));
        if (!refund.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("No refund found for booking " + bookingId);
        }
        return refund;
    }
}
