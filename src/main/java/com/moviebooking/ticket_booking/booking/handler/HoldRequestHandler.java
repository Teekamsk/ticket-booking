package com.moviebooking.ticket_booking.booking.handler;

import com.moviebooking.ticket_booking.booking.dto.CheckoutResponse;
import com.moviebooking.ticket_booking.booking.dto.CreateHoldRequest;
import com.moviebooking.ticket_booking.booking.dto.HoldResponse;
import com.moviebooking.ticket_booking.booking.mapper.BookingMapper;
import com.moviebooking.ticket_booking.booking.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldRequestHandler {

    private final SeatHoldService seatHoldService;

    public HoldResponse create(Long userId, CreateHoldRequest request) {
        return BookingMapper.toHoldResponse(
                seatHoldService.createHold(userId, request.showId(), request.seatIds()));
    }

    public HoldResponse get(Long userId, Long holdId) {
        return BookingMapper.toHoldResponse(seatHoldService.getHold(userId, holdId));
    }

    public void release(Long userId, Long holdId) {
        seatHoldService.releaseHold(userId, holdId);
    }

    public CheckoutResponse checkout(Long userId, Long holdId, String discountCode) {
        return BookingMapper.toCheckoutResponse(seatHoldService.checkout(userId, holdId, discountCode));
    }
}
