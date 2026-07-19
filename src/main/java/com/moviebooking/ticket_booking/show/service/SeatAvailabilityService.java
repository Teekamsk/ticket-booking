package com.moviebooking.ticket_booking.show.service;

import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowSeat;
import com.moviebooking.ticket_booking.show.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Builds the live seat map for a show (AVAILABLE/HELD/BOOKED per seat). */
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {

    private final ShowService showService;
    private final ShowSeatRepository showSeatRepository;

    @Transactional(readOnly = true)
    public SeatMap getSeatMap(Long showId) {
        Show show = showService.getOrThrow(showId);
        List<ShowSeat> seats = showSeatRepository.findByShowIdOrderByRowLabelAscSeatNumberAsc(showId);
        return new SeatMap(show, seats);
    }
}
