package com.moviebooking.ticket_booking.payment;

import com.moviebooking.ticket_booking.booking.AbstractBookingIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Forced-failure gateway: the payment is FAILED and the booking remains PENDING with seats still HELD. */
@TestPropertySource(properties = "app.payment.failure-rate=1.0")
class PaymentFailureTest extends AbstractBookingIntegrationTest {

    @Test
    void failedPayment_bookingStaysPending_seatsStillHeld() throws Exception {
        ShowFixture show = createShow(2);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);

        mockMvc.perform(authed(post("/api/v1/payments"), customer)
                        .content("{\"bookingId\":%d,\"method\":\"CARD\",\"idempotencyKey\":\"f-%d\"}".formatted(bookingId, System.nanoTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAILED")));

        mockMvc.perform(authed(get("/api/v1/bookings/" + bookingId), customer))
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("HELD")));
    }
}
