package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.entity.Booking;
import com.moviebooking.ticket_booking.booking.entity.BookingStatus;
import com.moviebooking.ticket_booking.booking.entity.Cancellation;
import com.moviebooking.ticket_booking.booking.entity.Ticket;
import com.moviebooking.ticket_booking.booking.entity.TicketStatus;
import com.moviebooking.ticket_booking.booking.event.BookingCancelledEvent;
import com.moviebooking.ticket_booking.booking.repository.BookingRepository;
import com.moviebooking.ticket_booking.booking.repository.CancellationRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.discount.api.DiscountApplicationService;
import com.moviebooking.ticket_booking.refund.api.RefundEvaluationService;
import com.moviebooking.ticket_booking.show.api.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;

/**
 * Customer cancellation of a CONFIRMED booking: applies the refund-policy gap check, frees the
 * seats, cancels tickets, records the cancellation, and publishes an event for the refund payout
 * (Phase 8). The refund itself is not created here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationService {

    private final BookingRepository bookingRepository;
    private final CancellationRepository cancellationRepository;
    private final SeatReservationService seatReservationService;
    private final RefundEvaluationService refundEvaluationService;
    private final DiscountApplicationService discountApplicationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingView cancel(Long userId, Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking " + bookingId + " not found");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Only confirmed bookings can be cancelled");
        }
        if (cancellationRepository.existsByBookingId(bookingId)) {
            throw new BusinessRuleException("Booking has already been cancelled");
        }
        long minutesBeforeShow = Duration.between(Instant.now(), booking.getStartTime()).toMinutes();
        if (minutesBeforeShow <= 0) {
            throw new BusinessRuleException("The show has already started");
        }
        OptionalInt refundPercent = refundEvaluationService.evaluate(booking.getRefundPolicyId(), minutesBeforeShow);
        if (refundPercent.isEmpty()) {
            throw new BusinessRuleException("Cancellation is not permitted this close to showtime");
        }

        booking.getTickets().forEach(ticket -> ticket.setStatus(TicketStatus.CANCELLED));
        List<Long> seatIds = booking.getTickets().stream().map(Ticket::getSeatId).toList();
        seatReservationService.releaseSeats(booking.getShowId(), seatIds);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());

        Cancellation cancellation = new Cancellation();
        cancellation.setBookingId(bookingId);
        cancellation.setCancelledBy(userId);
        cancellation.setReason(reason);
        cancellation.setRefundPercentApplied(refundPercent.getAsInt());
        cancellationRepository.save(cancellation);

        if (booking.getDiscountId() != null) {
            discountApplicationService.releaseRedemption(bookingId);
        }
        eventPublisher.publishEvent(new BookingCancelledEvent(bookingId, userId,
                refundPercent.getAsInt(), booking.getPayableAmount()));
        log.info("Booking {} cancelled by user {} (refund {}%)", bookingId, userId, refundPercent.getAsInt());

        List<SeatLine> seats = booking.getTickets().stream()
                .map(t -> new SeatLine(t.getSeatId(), t.getRowLabel(), t.getSeatNumber(),
                        t.getSeatType(), t.getPrice(), t.getStatus().name()))
                .toList();
        return new BookingView(booking, seats);
    }
}
