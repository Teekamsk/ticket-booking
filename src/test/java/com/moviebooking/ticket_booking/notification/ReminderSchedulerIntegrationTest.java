package com.moviebooking.ticket_booking.notification;

import com.moviebooking.ticket_booking.booking.AbstractBookingIntegrationTest;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.repository.NotificationRepository;
import com.moviebooking.ticket_booking.notification.scheduler.ReminderScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReminderSchedulerIntegrationTest extends AbstractBookingIntegrationTest {

    @Autowired
    private ReminderScheduler reminderScheduler;
    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void upcomingConfirmedBooking_getsOneReminderPerChannel_deduped() throws Exception {
        ShowFixture show = createShow(1, Instant.now().plus(1, ChronoUnit.HOURS));
        String customer = registerCustomer();
        long holdId = Long.parseLong(holdSeats(customer, show.showId(), List.of(show.seatIds().get(0))));
        long bookingId = createBooking(customer, holdId, null);
        mockMvc.perform(authed(post("/api/v1/payments"), customer)
                        .content("{\"bookingId\":%d,\"method\":\"UPI\",\"idempotencyKey\":\"rk-%d\"}"
                                .formatted(bookingId, System.nanoTime())))
                .andExpect(status().isOk());

        reminderScheduler.sendReminders();
        assertThat(notificationRepository.countByBookingIdAndType(bookingId, NotificationType.SHOW_REMINDER))
                .isEqualTo(2); // EMAIL + PUSH

        // Running again does not duplicate reminders
        reminderScheduler.sendReminders();
        assertThat(notificationRepository.countByBookingIdAndType(bookingId, NotificationType.SHOW_REMINDER))
                .isEqualTo(2);
    }
}
