package com.moviebooking.ticket_booking.refund.handler;

import com.moviebooking.ticket_booking.refund.dto.RefundResponse;
import com.moviebooking.ticket_booking.refund.mapper.RefundMapper;
import com.moviebooking.ticket_booking.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundRequestHandler {

    private final RefundService refundService;

    public RefundResponse getForBooking(Long userId, Long bookingId) {
        return RefundMapper.toRefundResponse(refundService.getForBooking(userId, bookingId));
    }
}
