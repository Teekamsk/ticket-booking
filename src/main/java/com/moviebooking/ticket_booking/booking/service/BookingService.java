package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.entity.Booking;
import com.moviebooking.ticket_booking.booking.entity.BookingStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.repository.BookingRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.discount.api.DiscountApplicationService;
import com.moviebooking.ticket_booking.discount.api.DiscountResult;
import com.moviebooking.ticket_booking.show.api.LockedSeat;
import com.moviebooking.ticket_booking.show.api.SeatReservationService;
import com.moviebooking.ticket_booking.show.api.ShowSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;

/** Booking creation (PENDING_PAYMENT, amounts frozen) and read queries. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String REF_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final SeatHoldService seatHoldService;
    private final SeatReservationService seatReservationService;
    private final DiscountApplicationService discountApplicationService;

    @Transactional
    public BookingView createBooking(Long userId, Long holdId, String discountCode) {
        SeatHold hold = seatHoldService.requireActiveHold(userId, holdId);
        if (bookingRepository.existsByHoldId(holdId)) {
            throw new BusinessRuleException("A booking already exists for this hold");
        }
        long total = hold.getTotalAmount();
        Long discountId = null;
        long discountAmount = 0;
        if (StringUtils.hasText(discountCode)) {
            DiscountResult result = discountApplicationService.validateAndCompute(discountCode, userId, total);
            discountId = result.discountId();
            discountAmount = result.discountAmount();
        }
        ShowSnapshot snapshot = seatReservationService.getShowSnapshot(hold.getShowId());

        Booking booking = new Booking();
        booking.setBookingRef(generateRef());
        booking.setUserId(userId);
        booking.setShowId(hold.getShowId());
        booking.setHoldId(holdId);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setTotalAmount(total);
        booking.setDiscountId(discountId);
        booking.setDiscountAmount(discountAmount);
        booking.setPayableAmount(total - discountAmount);
        booking.setMovieTitle(snapshot.movieTitle());
        booking.setScreenName(snapshot.screenName());
        booking.setTheatreName(snapshot.theatreName());
        booking.setStartTime(snapshot.startTime());
        booking.setRefundPolicyId(snapshot.refundPolicyId());
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} ({}) created PENDING_PAYMENT by user {}", saved.getId(), saved.getBookingRef(), userId);
        return new BookingView(saved, seatLinesFromHold(holdId));
    }

    @Transactional(readOnly = true)
    public BookingView getBooking(Long userId, Long bookingId) {
        Booking booking = ownedBooking(userId, bookingId);
        return new BookingView(booking, seatLinesFor(booking));
    }

    @Transactional(readOnly = true)
    public Page<Booking> listBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByIdDesc(userId, pageable);
    }

    private Booking ownedBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));
        if (!booking.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking " + bookingId + " not found");
        }
        return booking;
    }

    private List<SeatLine> seatLinesFor(Booking booking) {
        if (!booking.getTickets().isEmpty()) {
            return booking.getTickets().stream()
                    .map(t -> new SeatLine(t.getSeatId(), t.getRowLabel(), t.getSeatNumber(),
                            t.getSeatType(), t.getPrice(), t.getStatus().name()))
                    .toList();
        }
        return seatLinesFromHold(booking.getHoldId());
    }

    private List<SeatLine> seatLinesFromHold(Long holdId) {
        return seatReservationService.getSeatsByHold(holdId).stream()
                .map(s -> new SeatLine(s.seatId(), s.rowLabel(), s.seatNumber(), s.seatType(), s.price(), "HELD"))
                .toList();
    }

    private String generateRef() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("BK");
            for (int i = 0; i < 10; i++) {
                sb.append(REF_CHARS.charAt(random.nextInt(REF_CHARS.length())));
            }
            String ref = sb.toString();
            if (!bookingRepository.existsByBookingRef(ref)) {
                return ref;
            }
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }
}
