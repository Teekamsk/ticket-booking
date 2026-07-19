package com.moviebooking.ticket_booking.booking.api;

import com.moviebooking.ticket_booking.booking.entity.Booking;
import com.moviebooking.ticket_booking.booking.entity.BookingStatus;
import com.moviebooking.ticket_booking.booking.entity.HoldStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.entity.Ticket;
import com.moviebooking.ticket_booking.booking.entity.TicketStatus;
import com.moviebooking.ticket_booking.booking.event.BookingConfirmedEvent;
import com.moviebooking.ticket_booking.booking.exception.HoldExpiredException;
import com.moviebooking.ticket_booking.booking.repository.BookingRepository;
import com.moviebooking.ticket_booking.booking.repository.SeatHoldRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.discount.api.DiscountApplicationService;
import com.moviebooking.ticket_booking.show.api.LockedSeat;
import com.moviebooking.ticket_booking.show.api.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Published facade the payment module (Phase 7) calls on successful payment: creates tickets,
 * flips ShowSeats to BOOKED, converts the hold, records discount usage, and publishes an event.
 * The partial unique index on tickets is the final DB safety net against double-allocation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConfirmationService {

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatReservationService seatReservationService;
    private final DiscountApplicationService discountApplicationService;
    private final ApplicationEventPublisher eventPublisher;

    /** Payment-facing: validates the booking is the caller's and awaiting payment, returns its amount. */
    @Transactional(readOnly = true)
    public PayableBooking loadPayable(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking " + bookingId + " not found");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("Booking is not awaiting payment");
        }
        return new PayableBooking(bookingId, booking.getPayableAmount());
    }

    @Transactional
    public void confirm(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return; // idempotent
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("Booking cannot be confirmed in status " + booking.getStatus());
        }
        SeatHold hold = seatHoldRepository.findById(booking.getHoldId())
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found for booking"));
        if (hold.getStatus() != HoldStatus.ACTIVE || hold.isExpired(Instant.now())) {
            throw new HoldExpiredException("Hold expired before payment; booking cannot be confirmed");
        }
        List<LockedSeat> seats = seatReservationService.getSeatsByHold(hold.getId());
        if (seats.isEmpty()) {
            throw new HoldExpiredException("Held seats are no longer available");
        }

        for (LockedSeat seat : seats) {
            Ticket ticket = new Ticket();
            ticket.setShowId(booking.getShowId());
            ticket.setSeatId(seat.seatId());
            ticket.setRowLabel(seat.rowLabel());
            ticket.setSeatNumber(seat.seatNumber());
            ticket.setSeatType(seat.seatType());
            ticket.setPrice(seat.price());
            ticket.setStatus(TicketStatus.ACTIVE);
            booking.addTicket(ticket);
        }
        seatReservationService.markBooked(seats.stream().map(LockedSeat::showSeatId).toList());
        hold.setStatus(HoldStatus.CONVERTED);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        bookingRepository.save(booking);

        if (booking.getDiscountId() != null) {
            discountApplicationService.recordRedemption(booking.getDiscountId(), booking.getUserId(),
                    booking.getId(), booking.getDiscountAmount());
        }
        eventPublisher.publishEvent(
                new BookingConfirmedEvent(booking.getId(), booking.getUserId(), booking.getBookingRef()));
        log.info("Booking {} confirmed with {} tickets", bookingId, seats.size());
    }
}
