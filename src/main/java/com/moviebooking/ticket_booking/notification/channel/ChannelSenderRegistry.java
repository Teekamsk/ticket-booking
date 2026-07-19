package com.moviebooking.ticket_booking.notification.channel;

import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the right {@link ChannelSender} for a channel. Auto-discovers all senders on the classpath. */
@Component
public class ChannelSenderRegistry {

    private final Map<NotificationChannel, ChannelSender> byChannel;

    public ChannelSenderRegistry(List<ChannelSender> senders) {
        this.byChannel = senders.stream()
                .collect(Collectors.toMap(ChannelSender::channel, Function.identity()));
    }

    public Optional<ChannelSender> forChannel(NotificationChannel channel) {
        return Optional.ofNullable(byChannel.get(channel));
    }
}
