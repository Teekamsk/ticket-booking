package com.moviebooking.ticket_booking.notification.channel;

import com.moviebooking.ticket_booking.notification.entity.Notification;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Stub email sender — logs instead of sending. */
@Slf4j
@Component
public class EmailChannelSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[EMAIL] to user {}: {}", notification.getUserId(), notification.getPayload());
        return true;
    }
}
