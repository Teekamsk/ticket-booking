package com.moviebooking.ticket_booking.booking.handler;

import com.moviebooking.ticket_booking.booking.dto.BookingHistoryResponse;
import com.moviebooking.ticket_booking.booking.dto.BookingResponse;
import com.moviebooking.ticket_booking.booking.dto.BookingSummaryResponse;
import com.moviebooking.ticket_booking.booking.dto.CreateBookingRequest;
import com.moviebooking.ticket_booking.booking.entity.Booking;
import com.moviebooking.ticket_booking.booking.mapper.BookingMapper;
import com.moviebooking.ticket_booking.booking.service.BookingService;
import com.moviebooking.ticket_booking.booking.service.CancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingRequestHandler {

    private final BookingService bookingService;
    private final CancellationService cancellationService;

    public BookingResponse create(Long userId, CreateBookingRequest request) {
        return BookingMapper.toBookingResponse(
                bookingService.createBooking(userId, request.holdId(), request.discountCode()));
    }

    public BookingResponse get(Long userId, Long bookingId) {
        return BookingMapper.toBookingResponse(bookingService.getBooking(userId, bookingId));
    }

    public BookingHistoryResponse list(Long userId, int page, int size) {
        Page<Booking> result = bookingService.listBookings(userId, PageRequest.of(page, size));
        List<BookingSummaryResponse> content = result.getContent().stream()
                .map(BookingMapper::toSummary).toList();
        return new BookingHistoryResponse(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public BookingResponse cancel(Long userId, Long bookingId, String reason) {
        return BookingMapper.toBookingResponse(cancellationService.cancel(userId, bookingId, reason));
    }
}
