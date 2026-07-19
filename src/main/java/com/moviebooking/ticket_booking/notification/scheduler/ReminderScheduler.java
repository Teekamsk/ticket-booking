package com.moviebooking.ticket_booking.notification.scheduler;

import com.moviebooking.ticket_booking.booking.api.BookingReminderService;
import com.moviebooking.ticket_booking.booking.api.UpcomingBooking;
import com.moviebooking.ticket_booking.notification.config.NotificationProperties;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.repository.NotificationRepository;
import com.moviebooking.ticket_booking.notification.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Sends a one-time SHOW_REMINDER for confirmed bookings whose show starts within the window. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final List<NotificationChannel> CHANNELS =
            List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH);

    private final BookingReminderService bookingReminderService;
    private final CommunicationService communicationService;
    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;

    @Scheduled(
            initialDelayString = "${app.notification.reminder-initial-delay-ms:60000}",
            fixedDelayString = "${app.notification.reminder-interval-ms:3600000}")
    public void sendReminders() {
        List<UpcomingBooking> upcoming = bookingReminderService.findUpcoming(properties.reminderWindow());
        int sent = 0;
        for (UpcomingBooking booking : upcoming) {
            if (notificationRepository.existsByBookingIdAndType(booking.bookingId(), NotificationType.SHOW_REMINDER)) {
                continue;
            }
            communicationService.dispatch(booking.userId(), booking.bookingId(), NotificationType.SHOW_REMINDER,
                    "Reminder: '" + booking.movieTitle() + "' starts at " + booking.startTime()
                            + ". Booking " + booking.bookingRef() + ".", CHANNELS);
            sent++;
        }
        if (sent > 0) {
            log.info("Sent show reminders for {} booking(s)", sent);
        }
    }
}
