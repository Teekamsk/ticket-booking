package com.moviebooking.ticket_booking.booking.repository;

import com.moviebooking.ticket_booking.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = "tickets")
    Optional<Booking> findById(Long id);

    Page<Booking> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    boolean existsByBookingRef(String bookingRef);

    boolean existsByHoldId(Long holdId);
}
