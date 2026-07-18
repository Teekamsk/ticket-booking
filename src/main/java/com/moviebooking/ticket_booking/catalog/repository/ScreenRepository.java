package com.moviebooking.ticket_booking.catalog.repository;

import com.moviebooking.ticket_booking.catalog.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    boolean existsByTheatreIdAndNameIgnoreCase(Long theatreId, String name);
}
