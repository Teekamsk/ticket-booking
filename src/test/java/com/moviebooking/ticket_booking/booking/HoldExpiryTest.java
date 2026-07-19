package com.moviebooking.ticket_booking.booking;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With a ~immediately-expiring TTL, using the hold (checkout/book) is rejected with 410. */
@TestPropertySource(properties = "app.booking.hold-ttl=1ms")
class HoldExpiryTest extends AbstractBookingIntegrationTest {

    @Test
    void expiredHold_checkoutAndBook_return410() throws Exception {
        ShowFixture show = createShow(1);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));

        mockMvc.perform(authed(get("/api/v1/holds/" + holdId + "/checkout"), customer))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode", is("HOLD_EXPIRED")));

        mockMvc.perform(authed(post("/api/v1/bookings"), customer)
                        .content("{\"holdId\":%d}".formatted(holdId)))
                .andExpect(status().isGone());
    }
}
