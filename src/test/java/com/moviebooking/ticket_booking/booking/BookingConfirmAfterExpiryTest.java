package com.moviebooking.ticket_booking.booking;

import com.moviebooking.ticket_booking.booking.api.BookingConfirmationService;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.exception.HoldExpiredException;
import com.moviebooking.ticket_booking.booking.repository.SeatHoldRepository;
import com.moviebooking.ticket_booking.booking.service.HoldExpirySweeper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Edge case: the hold expires after a PENDING_PAYMENT booking is created but before confirmation.
 * A late "payment success" must not confirm a booking whose seats are gone — the booking stays
 * PENDING and (once swept) the seats return to AVAILABLE for other customers.
 */
class BookingConfirmAfterExpiryTest extends AbstractBookingIntegrationTest {

    @Autowired
    private SeatHoldRepository seatHoldRepository;
    @Autowired
    private HoldExpirySweeper holdExpirySweeper;
    @Autowired
    private BookingConfirmationService bookingConfirmationService;

    @Test
    void confirmAfterHoldExpiredAndSwept_rejected_bookingStaysPending_seatsFreed() throws Exception {
        ShowFixture show = createShow(1);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);

        // The hold's TTL lapses, and the sweeper reclaims the seats before payment is confirmed.
        expireHold(holdId);
        holdExpirySweeper.sweepExpiredHolds();

        // What the payment module calls on success — it must refuse the expired hold.
        assertThatThrownBy(() -> bookingConfirmationService.confirm(bookingId))
                .isInstanceOf(HoldExpiredException.class);

        mockMvc.perform(authed(get("/api/v1/bookings/" + bookingId), customer))
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("AVAILABLE")));
    }

    @Test
    void confirmWhenHoldExpiredByTimeButNotYetSwept_rejected() throws Exception {
        ShowFixture show = createShow(1);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);

        // Hold is past its expiry but the sweeper has not run yet (status still ACTIVE).
        expireHold(holdId);

        assertThatThrownBy(() -> bookingConfirmationService.confirm(bookingId))
                .isInstanceOf(HoldExpiredException.class);

        mockMvc.perform(authed(get("/api/v1/bookings/" + bookingId), customer))
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
    }

    private void expireHold(long holdId) {
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(Instant.now().minusSeconds(30));
        seatHoldRepository.save(hold);
    }
}
