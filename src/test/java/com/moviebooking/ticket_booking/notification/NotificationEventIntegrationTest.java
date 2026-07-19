package com.moviebooking.ticket_booking.notification;

import com.moviebooking.ticket_booking.booking.AbstractBookingIntegrationTest;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.repository.NotificationRepository;
import com.moviebooking.ticket_booking.refund.entity.Refund;
import com.moviebooking.ticket_booking.refund.entity.RefundStatus;
import com.moviebooking.ticket_booking.refund.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Payment/cancellation drive refunds and multi-channel notifications through the async event chain. */
class NotificationEventIntegrationTest extends AbstractBookingIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RefundRepository refundRepository;

    @Test
    void payThenCancel_createsRefundAndNotificationsOnBothChannels() throws Exception {
        ShowFixture show = createShow(1);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);
        pay(customer, bookingId);

        // Confirmation → BOOKING_CONFIRMED on EMAIL + PUSH
        await(() -> notificationRepository.countByBookingIdAndType(bookingId, NotificationType.BOOKING_CONFIRMED) == 2);

        // Cancel → refund payout + cancellation/refund notifications
        mockMvc.perform(authed(post("/api/v1/bookings/" + bookingId + "/cancel"), customer).content("{}"))
                .andExpect(status().isOk());

        await(() -> refundRepository.findByBookingId(bookingId)
                .filter(r -> r.getStatus() == RefundStatus.PROCESSED).isPresent());
        Refund refund = refundRepository.findByBookingId(bookingId).orElseThrow();
        assertThat(refund.getAmount()).isEqualTo(10000); // 50% of 20000

        await(() -> notificationRepository.countByBookingIdAndType(bookingId, NotificationType.BOOKING_CANCELLED) == 2);
        await(() -> notificationRepository.countByBookingIdAndType(bookingId, NotificationType.REFUND_PROCESSED) == 2);

        // Refund is visible to its owner, hidden from others
        mockMvc.perform(authed(get("/api/v1/refunds").param("bookingId", String.valueOf(bookingId)), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));
        mockMvc.perform(authed(get("/api/v1/refunds").param("bookingId", String.valueOf(bookingId)), registerCustomer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void refund_forBookingNeverCancelled_404() throws Exception {
        ShowFixture show = createShow(1);
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);
        pay(customer, bookingId);

        mockMvc.perform(authed(get("/api/v1/refunds").param("bookingId", String.valueOf(bookingId)), customer))
                .andExpect(status().isNotFound());
    }

    private void pay(String token, long bookingId) throws Exception {
        mockMvc.perform(authed(post("/api/v1/payments"), token)
                        .content("{\"bookingId\":%d,\"method\":\"UPI\",\"idempotencyKey\":\"nk-%d\"}"
                                .formatted(bookingId, System.nanoTime())))
                .andExpect(status().isOk());
    }

    private void await(Callable<Boolean> condition) throws Exception {
        for (int i = 0; i < 50; i++) {
            if (Boolean.TRUE.equals(condition.call())) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Async condition not met within timeout");
    }
}
