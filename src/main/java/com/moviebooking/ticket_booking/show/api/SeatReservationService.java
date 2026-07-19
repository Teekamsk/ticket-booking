package com.moviebooking.ticket_booking.show.api;

import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowSeat;
import com.moviebooking.ticket_booking.show.entity.ShowStatus;
import com.moviebooking.ticket_booking.show.repository.ShowRepository;
import com.moviebooking.ticket_booking.show.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Published facade the booking module uses to lock and transition ShowSeats (the show module owns
 * the table). Booking calls these inside its own transaction, so the FOR UPDATE lock spans the
 * whole hold/confirm operation. Booking decides <em>whether</em> to hold; this service only executes.
 */
@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    /** Locks the requested seats (FOR UPDATE) after checking the show is open, returning their state. */
    @Transactional
    public List<LockedSeat> lockSeats(Long showId, List<Long> seatIds) {
        Show show = getShow(showId);
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new BusinessRuleException("Show is not open for booking");
        }
        if (!show.getStartTime().isAfter(Instant.now())) {
            throw new BusinessRuleException("Show has already started");
        }
        List<ShowSeat> locked = showSeatRepository.lockSeats(showId, seatIds);
        if (locked.size() != new HashSet<>(seatIds).size()) {
            throw new ResourceNotFoundException("One or more seats do not exist for this show");
        }
        Map<String, Long> priceByType = priceByType(show);
        return locked.stream().map(seat -> toLockedSeat(seat, priceByType)).toList();
    }

    @Transactional
    public void markHeld(List<Long> showSeatIds, Long holdId) {
        showSeatRepository.markHeld(showSeatIds, holdId);
    }

    @Transactional
    public void markBooked(List<Long> showSeatIds) {
        showSeatRepository.markBooked(showSeatIds);
    }

    @Transactional
    public void releaseByHold(Long holdId) {
        showSeatRepository.releaseByHold(holdId);
    }

    @Transactional
    public void releaseSeats(Long showId, List<Long> seatIds) {
        showSeatRepository.releaseByShowAndSeats(showId, seatIds);
    }

    @Transactional(readOnly = true)
    public List<LockedSeat> getSeatsByHold(Long holdId) {
        List<ShowSeat> seats = showSeatRepository.findByHoldId(holdId);
        if (seats.isEmpty()) {
            return List.of();
        }
        Map<String, Long> priceByType = priceByType(seats.get(0).getShow());
        return seats.stream().map(seat -> toLockedSeat(seat, priceByType)).toList();
    }

    @Transactional(readOnly = true)
    public ShowSnapshot getShowSnapshot(Long showId) {
        Show show = getShow(showId);
        return new ShowSnapshot(show.getId(), show.getMovieTitle(), show.getScreenName(),
                show.getTheatreName(), show.getCityName(), show.getStartTime(), show.getEndTime(),
                show.getRefundPolicyId(), show.getStatus());
    }

    private Show getShow(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));
    }

    private Map<String, Long> priceByType(Show show) {
        return show.getPricing().stream()
                .collect(Collectors.toMap(p -> p.getSeatType(), p -> p.getPrice()));
    }

    private LockedSeat toLockedSeat(ShowSeat seat, Map<String, Long> priceByType) {
        return new LockedSeat(seat.getId(), seat.getSeatId(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getSeatType(), seat.getStatus(), seat.getHoldId(),
                priceByType.getOrDefault(seat.getSeatType(), 0L));
    }
}
