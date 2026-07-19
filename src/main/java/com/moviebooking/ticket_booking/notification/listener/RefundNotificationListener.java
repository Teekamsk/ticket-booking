package com.moviebooking.ticket_booking.notification.listener;

import com.moviebooking.ticket_booking.common.util.MoneyUtil;
import com.moviebooking.ticket_booking.notification.entity.NotificationChannel;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import com.moviebooking.ticket_booking.notification.service.CommunicationService;
import com.moviebooking.ticket_booking.refund.event.RefundProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RefundNotificationListener {

    private static final List<NotificationChannel> CHANNELS =
            List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH);

    private final CommunicationService communicationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundProcessed(RefundProcessedEvent event) {
        communicationService.dispatch(event.userId(), event.bookingId(), NotificationType.REFUND_PROCESSED,
                "A refund of " + MoneyUtil.format(event.amount()) + " for booking " + event.bookingRef()
                        + " has been processed.", CHANNELS);
    }
}
