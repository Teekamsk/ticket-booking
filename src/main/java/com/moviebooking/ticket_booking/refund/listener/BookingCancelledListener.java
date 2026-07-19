package com.moviebooking.ticket_booking.refund.listener;

import com.moviebooking.ticket_booking.booking.event.BookingCancelledEvent;
import com.moviebooking.ticket_booking.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Processes the refund after the cancellation transaction commits, off the request thread. */
@Component
@RequiredArgsConstructor
public class BookingCancelledListener {

    private final RefundService refundService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        refundService.processRefund(event);
    }
}
