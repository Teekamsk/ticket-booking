package com.moviebooking.ticket_booking.notification.service;

import com.moviebooking.ticket_booking.notification.channel.ChannelSender;
import com.moviebooking.ticket_booking.notification.channel.ChannelSenderRegistry;
import com.moviebooking.ticket_booking.notification.entity.Notification;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import com.moviebooking.ticket_booking.notification.entity.NotificationStatus;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ChannelSenderRegistry channelSenderRegistry;

    @InjectMocks
    private CommunicationService communicationService;

    @Test
    void dispatch_perChannel_persistsAndMarksSent() {
        ChannelSender email = mock(ChannelSender.class);
        ChannelSender push = mock(ChannelSender.class);
        when(email.send(any())).thenReturn(true);
        when(push.send(any())).thenReturn(true);
        when(channelSenderRegistry.forChannel(NotificationChannel.EMAIL)).thenReturn(Optional.of(email));
        when(channelSenderRegistry.forChannel(NotificationChannel.PUSH)).thenReturn(Optional.of(push));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        communicationService.dispatch(7L, 2L, NotificationType.BOOKING_CONFIRMED, "hi",
                List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getStatus)
                .containsOnly(NotificationStatus.SENT);
        assertThat(captor.getAllValues()).extracting(Notification::getChannel)
                .containsExactly(NotificationChannel.EMAIL, NotificationChannel.PUSH);
    }

    @Test
    void dispatch_senderFails_marksFailed() {
        ChannelSender email = mock(ChannelSender.class);
        when(email.send(any())).thenReturn(false);
        when(channelSenderRegistry.forChannel(NotificationChannel.EMAIL)).thenReturn(Optional.of(email));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        communicationService.dispatch(7L, 2L, NotificationType.REFUND_PROCESSED, "x",
                List.of(NotificationChannel.EMAIL));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getSentAt()).isNull();
    }

    @Test
    void dispatch_noSenderForChannel_marksFailed() {
        when(channelSenderRegistry.forChannel(NotificationChannel.SMS)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        communicationService.dispatch(7L, 2L, NotificationType.SHOW_REMINDER, "x",
                List.of(NotificationChannel.SMS));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
