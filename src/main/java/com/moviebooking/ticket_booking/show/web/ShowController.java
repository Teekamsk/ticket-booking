package com.moviebooking.ticket_booking.show.web;

import com.moviebooking.ticket_booking.show.dto.SeatMapResponse;
import com.moviebooking.ticket_booking.show.dto.ShowResponse;
import com.moviebooking.ticket_booking.show.dto.TheatreShowsResponse;
import com.moviebooking.ticket_booking.show.handler.ShowQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Public show browsing. No authentication required. */
@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowQueryHandler showQueryHandler;

    @GetMapping
    public List<TheatreShowsResponse> search(
            @RequestParam Long cityId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return showQueryHandler.search(cityId, movieId, date);
    }

    @GetMapping("/{id}")
    public ShowResponse detail(@PathVariable Long id) {
        return showQueryHandler.detail(id);
    }

    @GetMapping("/{id}/seats")
    public SeatMapResponse seats(@PathVariable Long id) {
        return showQueryHandler.seatMap(id);
    }
}
