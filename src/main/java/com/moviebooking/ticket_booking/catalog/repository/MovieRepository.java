package com.moviebooking.ticket_booking.catalog.repository;

import com.moviebooking.ticket_booking.catalog.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByActiveTrueOrderByTitleAsc();

    List<Movie> findByActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc(String title);
}
