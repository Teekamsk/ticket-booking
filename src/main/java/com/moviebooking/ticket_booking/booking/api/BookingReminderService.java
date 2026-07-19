package com.moviebooking.ticket_booking.booking.api;

import com.moviebooking.ticket_booking.booking.entity.BookingStatus;
import com.moviebooking.ticket_booking.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Published facade: confirmed bookings whose show starts within a window (for reminders). */
@Service
@RequiredArgsConstructor
public class BookingReminderService {

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<UpcomingBooking> findUpcoming(Duration within) {
        Instant now = Instant.now();
        return bookingRepository.findByStatusAndStartTimeBetween(BookingStatus.CONFIRMED, now, now.plus(within))
                .stream()
                .map(b -> new UpcomingBooking(b.getUserId(), b.getId(), b.getBookingRef(),
                        b.getMovieTitle(), b.getStartTime()))
                .toList();
    }
}
