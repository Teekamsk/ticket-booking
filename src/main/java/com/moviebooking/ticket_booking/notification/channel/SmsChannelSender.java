package com.moviebooking.ticket_booking.notification.channel;

import com.moviebooking.ticket_booking.notification.entity.Notification;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Stub SMS sender — logs instead of sending. Available but not used by default (see listeners). */
@Slf4j
@Component
public class SmsChannelSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[SMS] to user {}: {}", notification.getUserId(), notification.getPayload());
        return true;
    }
}
