package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.Certificate;
import com.moviebooking.ticket_booking.catalog.entity.Movie;
import com.moviebooking.ticket_booking.catalog.repository.MovieRepository;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional
    public Movie create(String title, String language, String genre, int durationMin,
                        Certificate certificate, LocalDate releaseDate) {
        Movie movie = new Movie();
        apply(movie, title, language, genre, durationMin, certificate, releaseDate);
        movie.setActive(true);
        Movie saved = movieRepository.save(movie);
        log.info("Created movie {} (id={})", saved.getTitle(), saved.getId());
        return saved;
    }

    @Transactional
    public Movie update(Long id, String title, String language, String genre, int durationMin,
                        Certificate certificate, LocalDate releaseDate) {
        Movie movie = getOrThrow(id);
        apply(movie, title, language, genre, durationMin, certificate, releaseDate);
        return movie;
    }

    @Transactional
    public void deactivate(Long id) {
        Movie movie = getOrThrow(id);
        movie.setActive(false);
        log.info("Deactivated movie id={}", id);
    }

    @Transactional(readOnly = true)
    public List<Movie> browse(String query) {
        if (StringUtils.hasText(query)) {
            return movieRepository.findByActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc(query.trim());
        }
        return movieRepository.findByActiveTrueOrderByTitleAsc();
    }

    public Movie getOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie " + id + " not found"));
    }

    private void apply(Movie movie, String title, String language, String genre, int durationMin,
                       Certificate certificate, LocalDate releaseDate) {
        movie.setTitle(title);
        movie.setLanguage(language);
        movie.setGenre(genre);
        movie.setDurationMin(durationMin);
        movie.setCertificate(certificate);
        movie.setReleaseDate(releaseDate);
    }
}
