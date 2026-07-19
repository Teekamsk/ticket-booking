package com.moviebooking.ticket_booking.booking.mapper;

import com.moviebooking.ticket_booking.booking.dto.BookingResponse;
import com.moviebooking.ticket_booking.booking.dto.BookingSummaryResponse;
import com.moviebooking.ticket_booking.booking.dto.CheckoutResponse;
import com.moviebooking.ticket_booking.booking.dto.HoldResponse;
import com.moviebooking.ticket_booking.booking.dto.SeatLineResponse;
import com.moviebooking.ticket_booking.booking.entity.Booking;
import com.moviebooking.ticket_booking.booking.entity.HoldStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.service.BookingView;
import com.moviebooking.ticket_booking.booking.service.CheckoutView;
import com.moviebooking.ticket_booking.booking.service.HoldView;
import com.moviebooking.ticket_booking.show.api.LockedSeat;

import java.time.Instant;
import java.util.List;

/** Entity/view → response DTO mapping for the booking module. */
public final class BookingMapper {

    private BookingMapper() {
    }

    public static HoldResponse toHoldResponse(HoldView view) {
        SeatHold hold = view.hold();
        return new HoldResponse(hold.getId(), hold.getShowId(), effectiveStatus(hold), hold.getExpiresAt(),
                secondsRemaining(hold), hold.getTotalAmount(), toSeatLines(view.seats()));
    }

    public static CheckoutResponse toCheckoutResponse(CheckoutView view) {
        SeatHold hold = view.hold();
        return new CheckoutResponse(hold.getId(), hold.getExpiresAt(), secondsRemaining(hold),
                view.totalAmount(), view.discountCode(), view.discountAmount(), view.payableAmount(),
                toSeatLines(view.seats()));
    }

    public static BookingResponse toBookingResponse(BookingView view) {
        Booking b = view.booking();
        List<SeatLineResponse> seats = view.seats().stream()
                .map(s -> new SeatLineResponse(s.seatId(), s.rowLabel(), s.seatNumber(), s.seatType(),
                        s.price(), s.status()))
                .toList();
        return new BookingResponse(b.getId(), b.getBookingRef(), b.getShowId(), b.getMovieTitle(),
                b.getScreenName(), b.getTheatreName(), b.getStartTime(), b.getStatus().name(),
                b.getTotalAmount(), b.getDiscountAmount(), b.getPayableAmount(),
                b.getConfirmedAt(), b.getCancelledAt(), seats);
    }

    public static BookingSummaryResponse toSummary(Booking b) {
        return new BookingSummaryResponse(b.getId(), b.getBookingRef(), b.getStatus().name(),
                b.getMovieTitle(), b.getStartTime(), b.getPayableAmount());
    }

    private static List<SeatLineResponse> toSeatLines(List<LockedSeat> seats) {
        return seats.stream()
                .map(s -> new SeatLineResponse(s.seatId(), s.rowLabel(), s.seatNumber(), s.seatType(),
                        s.price(), s.status().name()))
                .toList();
    }

    private static String effectiveStatus(SeatHold hold) {
        if (hold.getStatus() == HoldStatus.ACTIVE && hold.isExpired(Instant.now())) {
            return HoldStatus.EXPIRED.name();
        }
        return hold.getStatus().name();
    }

    private static long secondsRemaining(SeatHold hold) {
        long seconds = java.time.Duration.between(Instant.now(), hold.getExpiresAt()).getSeconds();
        return Math.max(0, seconds);
    }
}
