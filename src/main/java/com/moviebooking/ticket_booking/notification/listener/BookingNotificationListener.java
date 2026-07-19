package com.moviebooking.ticket_booking.notification.listener;

import com.moviebooking.ticket_booking.booking.event.BookingCancelledEvent;
import com.moviebooking.ticket_booking.booking.event.BookingConfirmedEvent;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/** Sends booking notifications after the originating transaction commits, off the request thread. */
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private static final List<NotificationChannel> CHANNELS =
            List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH);

    private final CommunicationService communicationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        communicationService.dispatch(event.userId(), event.bookingId(), NotificationType.BOOKING_CONFIRMED,
                "Your booking " + event.bookingRef() + " is confirmed.", CHANNELS);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        communicationService.dispatch(event.userId(), event.bookingId(), NotificationType.BOOKING_CANCELLED,
                "Your booking " + event.bookingRef() + " was cancelled (refund "
                        + event.refundPercentApplied() + "%).", CHANNELS);
    }
}
