package com.moviebooking.ticket_booking.show.service;

import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowStatus;
import com.moviebooking.ticket_booking.show.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** Public show search over the denormalized show schema — no catalog queries. */
@Service
@RequiredArgsConstructor
public class ShowSearchService {

    private static final Duration DEFAULT_WINDOW = Duration.ofDays(90);

    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public List<Show> search(Long cityId, Long movieId, LocalDate date) {
        Instant from;
        Instant to;
        if (date != null) {
            from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else {
            from = Instant.now();
            to = from.plus(DEFAULT_WINDOW);
        }
        return showRepository.search(cityId, movieId, ShowStatus.SCHEDULED, from, to);
    }
}
