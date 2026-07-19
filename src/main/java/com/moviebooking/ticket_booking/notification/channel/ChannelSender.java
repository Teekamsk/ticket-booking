package com.moviebooking.ticket_booking.notification.channel;

import com.moviebooking.ticket_booking.notification.entity.Notification;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;

/**
 * Delivers a notification over one channel. Add a new channel by dropping in a new implementation —
 * the registry picks it up automatically. All implementations are currently log-stubbed.
 */
public interface ChannelSender {

    NotificationChannel channel();

    boolean send(Notification notification);
}
