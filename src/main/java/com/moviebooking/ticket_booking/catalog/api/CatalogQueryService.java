package com.moviebooking.ticket_booking.catalog.api;

import com.moviebooking.ticket_booking.catalog.entity.City;
import com.moviebooking.ticket_booking.catalog.entity.Movie;
import com.moviebooking.ticket_booking.catalog.entity.Screen;
import com.moviebooking.ticket_booking.catalog.entity.Theatre;
import com.moviebooking.ticket_booking.catalog.repository.SeatRepository;
import com.moviebooking.ticket_booking.catalog.service.MovieService;
import com.moviebooking.ticket_booking.catalog.service.ScreenService;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only cross-module facade the {@code show} module uses to validate references and snapshot
 * catalog data at show-creation time. Keeps other modules off catalog's entities/repositories.
 */
@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final MovieService movieService;
    private final ScreenService screenService;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public MovieSummary getActiveMovie(Long movieId) {
        Movie movie = movieService.getOrThrow(movieId);
        if (!movie.isActive()) {
            throw new BusinessRuleException("Movie " + movieId + " is not active");
        }
        return new MovieSummary(movie.getId(), movie.getTitle(), movie.getLanguage(),
                movie.getCertificate().name(), movie.getDurationMin());
    }

    @Transactional(readOnly = true)
    public ScreenLocation getActiveScreenLocation(Long screenId) {
        Screen screen = screenService.getActiveOrThrow(screenId);
        Theatre theatre = screen.getTheatre();
        if (!theatre.isActive()) {
            throw new BusinessRuleException("Theatre " + theatre.getId() + " is not active");
        }
        City city = theatre.getCity();
        if (!city.isActive()) {
            throw new BusinessRuleException("City " + city.getId() + " is not active");
        }
        return new ScreenLocation(screen.getId(), screen.getName(),
                theatre.getId(), theatre.getName(), city.getId(), city.getName());
    }

    @Transactional(readOnly = true)
    public List<SeatView> getActiveSeats(Long screenId) {
        return seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId).stream()
                .filter(seat -> seat.isActive())
                .map(seat -> new SeatView(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(),
                        seat.getSeatType().name()))
                .toList();
    }
}
