package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.entity.HoldStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.repository.SeatHoldRepository;
import com.moviebooking.ticket_booking.show.api.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Bulk cleanup of expired holds. Correctness never depends on this (every read/use path checks
 * expiry lazily); it just returns leaked seats to AVAILABLE promptly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpirySweeper {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatReservationService seatReservationService;

    @Scheduled(fixedDelayString = "${app.booking.sweeper-interval-ms:30000}")
    @Transactional
    public void sweepExpiredHolds() {
        List<SeatHold> expired = seatHoldRepository.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, Instant.now());
        for (SeatHold hold : expired) {
            hold.setStatus(HoldStatus.EXPIRED);
            seatReservationService.releaseByHold(hold.getId());
        }
        if (!expired.isEmpty()) {
            log.info("Sweeper expired {} hold(s)", expired.size());
        }
    }
}
