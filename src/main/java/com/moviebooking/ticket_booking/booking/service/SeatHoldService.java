package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.config.BookingProperties;
import com.moviebooking.ticket_booking.booking.entity.HoldStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.exception.HoldExpiredException;
import com.moviebooking.ticket_booking.booking.exception.SeatUnavailableException;
import com.moviebooking.ticket_booking.booking.repository.SeatHoldRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.discount.api.DiscountApplicationService;
import com.moviebooking.ticket_booking.discount.api.DiscountResult;
import com.moviebooking.ticket_booking.show.api.LockedSeat;
import com.moviebooking.ticket_booking.show.api.SeatReservationService;
import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatReservationService seatReservationService;
    private final DiscountApplicationService discountApplicationService;
    private final BookingProperties bookingProperties;

    /**
     * Locks the requested seats (FOR UPDATE), verifies each is AVAILABLE or reclaimable from an
     * expired hold, then creates the hold and flips the seats to HELD — all in one transaction.
     * Any conflict rolls the whole thing back (nothing held) and surfaces as 409.
     */
    @Transactional
    public HoldView createHold(Long userId, Long showId, List<Long> seatIds) {
        List<Long> distinctSeatIds = seatIds.stream().distinct().toList();
        List<LockedSeat> locked = seatReservationService.lockSeats(showId, distinctSeatIds);
        Instant now = Instant.now();

        Set<Long> reclaimableHoldIds = new HashSet<>();
        for (LockedSeat seat : locked) {
            switch (seat.status()) {
                case AVAILABLE -> { /* free to take */ }
                case BOOKED -> throw seatTaken(seat);
                case HELD -> {
                    SeatHold existing = seat.holdId() == null ? null
                            : seatHoldRepository.findById(seat.holdId()).orElse(null);
                    if (existing != null && existing.getStatus() == HoldStatus.ACTIVE && !existing.isExpired(now)) {
                        throw seatTaken(seat);
                    }
                    reclaimableHoldIds.add(seat.holdId());
                }
            }
        }
        // Reclaim expired holds: expire them and release all their seats before we re-hold.
        for (Long holdId : reclaimableHoldIds) {
            seatHoldRepository.findById(holdId)
                    .filter(h -> h.getStatus() == HoldStatus.ACTIVE)
                    .ifPresent(h -> h.setStatus(HoldStatus.EXPIRED));
            seatReservationService.releaseByHold(holdId);
        }

        long total = locked.stream().mapToLong(LockedSeat::price).sum();
        SeatHold hold = new SeatHold();
        hold.setUserId(userId);
        hold.setShowId(showId);
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setExpiresAt(now.plus(bookingProperties.holdTtl()));
        hold.setTotalAmount(total);
        SeatHold saved = seatHoldRepository.save(hold);

        List<Long> showSeatIds = locked.stream().map(LockedSeat::showSeatId).toList();
        seatReservationService.markHeld(showSeatIds, saved.getId());
        log.info("Hold {} created by user {} for {} seats on show {}", saved.getId(), userId, locked.size(), showId);
        return new HoldView(saved, refreshStatuses(locked));
    }

    /** Non-mutating read: reports the hold with an effective (possibly expired) status. */
    @Transactional(readOnly = true)
    public HoldView getHold(Long userId, Long holdId) {
        SeatHold hold = ownedHold(userId, holdId);
        return new HoldView(hold, seatReservationService.getSeatsByHold(holdId));
    }

    @Transactional
    public void releaseHold(Long userId, Long holdId) {
        SeatHold hold = ownedHold(userId, holdId);
        if (hold.getStatus() == HoldStatus.CONVERTED) {
            throw new BusinessRuleException("Hold has already been converted to a booking");
        }
        if (hold.getStatus() == HoldStatus.ACTIVE) {
            hold.setStatus(HoldStatus.RELEASED);
            seatReservationService.releaseByHold(holdId);
            log.info("Hold {} released by user {}", holdId, userId);
        }
    }

    @Transactional(readOnly = true)
    public CheckoutView checkout(Long userId, Long holdId, String discountCode) {
        SeatHold hold = requireActiveHold(userId, holdId);
        List<LockedSeat> seats = seatReservationService.getSeatsByHold(holdId);
        long total = hold.getTotalAmount();
        long discountAmount = 0;
        if (StringUtils.hasText(discountCode)) {
            DiscountResult result = discountApplicationService.validateAndCompute(discountCode, userId, total);
            discountAmount = result.discountAmount();
        }
        return new CheckoutView(hold, seats, total, discountCode, discountAmount, total - discountAmount);
    }

    /** Used by booking creation: the hold must be the caller's, ACTIVE, and not expired. */
    @Transactional(readOnly = true)
    public SeatHold requireActiveHold(Long userId, Long holdId) {
        SeatHold hold = ownedHold(userId, holdId);
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new HoldExpiredException("Hold is no longer active");
        }
        if (hold.isExpired(Instant.now())) {
            throw new HoldExpiredException("Hold has expired");
        }
        return hold;
    }

    private SeatHold ownedHold(Long userId, Long holdId) {
        SeatHold hold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold " + holdId + " not found"));
        if (!hold.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Hold " + holdId + " not found");
        }
        return hold;
    }

    private SeatUnavailableException seatTaken(LockedSeat seat) {
        return new SeatUnavailableException("Seat " + seat.rowLabel() + seat.seatNumber() + " is not available");
    }

    /** Present the freshly-held seats as HELD in the response (locked snapshot was pre-hold). */
    private List<LockedSeat> refreshStatuses(List<LockedSeat> locked) {
        List<LockedSeat> result = new ArrayList<>(locked.size());
        for (LockedSeat s : locked) {
            result.add(new LockedSeat(s.showSeatId(), s.seatId(), s.rowLabel(), s.seatNumber(),
                    s.seatType(), ShowSeatStatus.HELD, s.holdId(), s.price()));
        }
        return result;
    }
}
