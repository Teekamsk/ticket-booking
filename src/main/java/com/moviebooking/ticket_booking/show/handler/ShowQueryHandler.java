package com.moviebooking.ticket_booking.show.handler;

import com.moviebooking.ticket_booking.show.dto.SeatMapResponse;
import com.moviebooking.ticket_booking.show.dto.ShowResponse;
import com.moviebooking.ticket_booking.show.dto.TheatreShowsResponse;
import com.moviebooking.ticket_booking.show.mapper.ShowMapper;
import com.moviebooking.ticket_booking.show.service.SeatAvailabilityService;
import com.moviebooking.ticket_booking.show.service.ShowSearchService;
import com.moviebooking.ticket_booking.show.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Public show queries: search (grouped by theatre), detail, live seat map. */
@Component
@RequiredArgsConstructor
public class ShowQueryHandler {

    private final ShowSearchService showSearchService;
    private final ShowService showService;
    private final SeatAvailabilityService seatAvailabilityService;

    public List<TheatreShowsResponse> search(Long cityId, Long movieId, LocalDate date) {
        return ShowMapper.groupByTheatre(showSearchService.search(cityId, movieId, date));
    }

    public ShowResponse detail(Long id) {
        return ShowMapper.toShowResponse(showService.getOrThrow(id));
    }

    public SeatMapResponse seatMap(Long id) {
        return ShowMapper.toSeatMapResponse(seatAvailabilityService.getSeatMap(id));
    }
}
