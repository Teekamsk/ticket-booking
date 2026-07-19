package com.moviebooking.ticket_booking.catalog.repository;

import com.moviebooking.ticket_booking.catalog.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<City> findByActiveTrueOrderByNameAsc();
}
