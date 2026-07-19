package com.moviebooking.ticket_booking.catalog.repository;

import com.moviebooking.ticket_booking.catalog.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    boolean existsByCityIdAndNameIgnoreCase(Long cityId, String name);
}
