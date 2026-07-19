package com.moviebooking.ticket_booking.payment;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.ticket_booking.booking.AbstractBookingIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentFlowIntegrationTest extends AbstractBookingIntegrationTest {

    @Test
    void payment_success_confirmsBookingAndBooksSeats() throws Exception {
        ShowFixture show = createShow(2);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);
        String key = "k-" + System.nanoTime();

        String payResponse = mockMvc.perform(authed(post("/api/v1/payments"), customer)
                        .content("{\"bookingId\":%d,\"method\":\"UPI\",\"idempotencyKey\":\"%s\"}".formatted(bookingId, key)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.amount", is(20000)))
                .andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(payResponse, "$.id")).longValue();

        mockMvc.perform(authed(get("/api/v1/bookings/" + bookingId), customer))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("BOOKED")));
        mockMvc.perform(authed(get("/api/v1/payments/" + paymentId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void payment_idempotentRetry_returnsSamePayment() throws Exception {
        ShowFixture show = createShow(2);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);
        String key = "k-" + System.nanoTime();
        String body = "{\"bookingId\":%d,\"method\":\"UPI\",\"idempotencyKey\":\"%s\"}".formatted(bookingId, key);

        long first = ((Number) JsonPath.read(mockMvc.perform(authed(post("/api/v1/payments"), customer).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        long second = ((Number) JsonPath.read(mockMvc.perform(authed(post("/api/v1/payments"), customer).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
    }

    @Test
    void payment_forAlreadyConfirmedBooking_422() throws Exception {
        ShowFixture show = createShow(2);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);
        mockMvc.perform(authed(post("/api/v1/payments"), customer)
                        .content("{\"bookingId\":%d,\"method\":\"UPI\",\"idempotencyKey\":\"k1-%d\"}".formatted(bookingId, System.nanoTime())))
                .andExpect(status().isOk());

        mockMvc.perform(authed(post("/api/v1/payments"), customer)
                        .content("{\"bookingId\":%d,\"method\":\"UPI\",\"idempotencyKey\":\"k2-%d\"}".formatted(bookingId, System.nanoTime())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    void payment_withoutToken_401() throws Exception {
        mockMvc.perform(post("/api/v1/payments").contentType(APPLICATION_JSON)
                        .content("{\"bookingId\":1,\"method\":\"UPI\",\"idempotencyKey\":\"k\"}"))
                .andExpect(status().isUnauthorized());
    }
}
