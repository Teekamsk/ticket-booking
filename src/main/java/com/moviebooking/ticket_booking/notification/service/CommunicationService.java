package com.moviebooking.ticket_booking.notification.service;

import com.moviebooking.ticket_booking.notification.channel.ChannelSenderRegistry;
import com.moviebooking.ticket_booking.notification.entity.Notification;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import com.moviebooking.ticket_booking.notification.entity.NotificationStatus;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Multi-channel communication dispatcher. Persists one {@link Notification} per channel and delivers
 * it via that channel's sender. Channel selection is passed in by the caller (a real system would use
 * per-user communication preferences, which are out of scope here).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationService {

    private final NotificationRepository notificationRepository;
    private final ChannelSenderRegistry channelSenderRegistry;

    @Transactional
    public void dispatch(Long userId, Long bookingId, NotificationType type, String payload,
                         List<NotificationChannel> channels) {
        for (NotificationChannel channel : channels) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setBookingId(bookingId);
            notification.setType(type);
            notification.setChannel(channel);
            notification.setPayload(payload);
            notification.setStatus(NotificationStatus.PENDING);

            boolean sent = channelSenderRegistry.forChannel(channel)
                    .map(sender -> sender.send(notification))
                    .orElseGet(() -> {
                        log.warn("No sender registered for channel {}", channel);
                        return false;
                    });
            notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
            if (sent) {
                notification.setSentAt(Instant.now());
            }
            notificationRepository.save(notification);
        }
    }
}
