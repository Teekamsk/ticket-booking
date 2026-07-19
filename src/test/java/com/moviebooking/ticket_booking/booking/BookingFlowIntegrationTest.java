package com.moviebooking.ticket_booking.booking;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.ticket_booking.booking.api.BookingConfirmationService;
import com.moviebooking.ticket_booking.booking.entity.Cancellation;
import com.moviebooking.ticket_booking.booking.repository.CancellationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingFlowIntegrationTest extends AbstractBookingIntegrationTest {

    @Autowired
    private BookingConfirmationService confirmationService;
    @Autowired
    private CancellationRepository cancellationRepository;

    @Test
    void hold_checkout_book_confirm_cancel() throws Exception {
        ShowFixture show = createShow(4);
        String customer = registerCustomer();
        List<Long> twoSeats = show.seatIds().subList(0, 2);

        long holdId = Long.parseLong(holdSeats(customer, show.showId(), twoSeats));

        // Checkout, total = 2 * 20000
        mockMvc.perform(authed(get("/api/v1/holds/" + holdId + "/checkout"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(40000)))
                .andExpect(jsonPath("$.payableAmount", is(40000)));

        // Create booking (PENDING_PAYMENT)
        long bookingId = createBooking(customer, holdId, null);
        mockMvc.perform(authed(get("/api/v1/bookings/" + bookingId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")))
                .andExpect(jsonPath("$.seats", org.hamcrest.Matchers.hasSize(2)));

        // Confirm (what the payment module will call on success)
        confirmationService.confirm(bookingId);
        mockMvc.perform(authed(get("/api/v1/bookings/" + bookingId), customer))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.seats[0].status", is("ACTIVE")));

        // Seats are now BOOKED on the show
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("BOOKED")));

        // Cancel (~2 days out → 50% tier)
        mockMvc.perform(authed(post("/api/v1/bookings/" + bookingId + "/cancel"), customer)
                        .content("{\"reason\":\"changed plans\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        Cancellation cancellation = cancellationRepository.findAll().stream()
                .filter(c -> c.getBookingId().equals(bookingId)).findFirst().orElseThrow();
        assertThat(cancellation.getRefundPercentApplied()).isEqualTo(50);

        // Seats released back to AVAILABLE
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("AVAILABLE")));
    }

    @Test
    void bookingSeatAgainAfterConfirm_conflicts() throws Exception {
        ShowFixture show = createShow(2);
        String a = registerCustomer();
        String b = registerCustomer();
        Long seat = show.seatIds().get(0);

        long holdA = Long.parseLong(holdSeats(a, show.showId(), List.of(seat)));
        long bookingA = createBooking(a, holdA, null);
        confirmationService.confirm(bookingA);

        // The seat is BOOKED — a fresh hold on it fails
        mockMvc.perform(authed(post("/api/v1/holds"), b)
                        .content("{\"showId\":%d,\"seatIds\":[%d]}".formatted(show.showId(), seat)))
                .andExpect(status().isConflict());
    }

    @Test
    void releaseHold_freesSeats() throws Exception {
        ShowFixture show = createShow(2);
        String customer = registerCustomer();
        Long seat = show.seatIds().get(0);
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(seat)));

        mockMvc.perform(authed(delete("/api/v1/holds/" + holdId), customer))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("AVAILABLE")));
    }

    @Test
    void adminCannotHold_forbidden() throws Exception {
        ShowFixture show = createShow(1);
        mockMvc.perform(authed(post("/api/v1/holds"), adminToken())
                        .content("{\"showId\":%d,\"seatIds\":[%d]}".formatted(show.showId(), show.seatIds().get(0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void hold_withoutToken_unauthorized() throws Exception {
        ShowFixture show = createShow(1);
        mockMvc.perform(post("/api/v1/holds")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"showId\":%d,\"seatIds\":[%d]}".formatted(show.showId(), show.seatIds().get(0))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_withDiscountCode_reducesPayable() throws Exception {
        ShowFixture show = createShow(2);
        String admin = adminToken();
        String code = "FLOW" + System.nanoTime();
        mockMvc.perform(authed(post("/api/v1/admin/discounts"), admin)
                        .content("{\"code\":\"%s\",\"discountType\":\"PERCENT\",\"value\":10,\"minOrderAmount\":0,\"validFrom\":\"2026-01-01T00:00:00Z\",\"validTo\":\"2030-01-01T00:00:00Z\"}".formatted(code)))
                .andExpect(status().isCreated());
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), show.seatIds().subList(0, 2)));

        mockMvc.perform(authed(get("/api/v1/holds/" + holdId + "/checkout").param("discountCode", code), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(40000)))
                .andExpect(jsonPath("$.discountAmount", is(4000)))
                .andExpect(jsonPath("$.payableAmount", is(36000)));
    }

    @Test
    void bookingHistory_isPagedAndOwnerScoped() throws Exception {
        ShowFixture show = createShow(2);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), show.seatIds().subList(0, 1)));
        createBooking(customer, holdId, null);

        String history = mockMvc.perform(authed(get("/api/v1/bookings").param("page", "0").param("size", "10"), customer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int total = ((Number) JsonPath.read(history, "$.totalElements")).intValue();
        assertThat(total).isEqualTo(1);
    }
}
