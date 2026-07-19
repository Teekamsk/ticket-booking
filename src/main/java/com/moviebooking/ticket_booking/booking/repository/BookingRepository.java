package com.moviebooking.ticket_booking.booking.repository;

import com.moviebooking.ticket_booking.booking.entity.Booking;
import com.moviebooking.ticket_booking.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = "tickets")
    Optional<Booking> findById(Long id);

    Page<Booking> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    boolean existsByBookingRef(String bookingRef);

    boolean existsByHoldId(Long holdId);

    List<Booking> findByStatusAndStartTimeBetween(BookingStatus status, Instant from, Instant to);
}
