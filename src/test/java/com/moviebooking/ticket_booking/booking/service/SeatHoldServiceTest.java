package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.config.BookingProperties;
import com.moviebooking.ticket_booking.booking.entity.HoldStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.booking.exception.SeatUnavailableException;
import com.moviebooking.ticket_booking.booking.repository.SeatHoldRepository;
import com.moviebooking.ticket_booking.discount.api.DiscountApplicationService;
import com.moviebooking.ticket_booking.show.api.LockedSeat;
import com.moviebooking.ticket_booking.show.api.SeatReservationService;
import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceTest {

    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private SeatReservationService seatReservationService;
    @Mock
    private DiscountApplicationService discountApplicationService;
    @Mock
    private BookingProperties bookingProperties;

    @InjectMocks
    private SeatHoldService seatHoldService;

    private LockedSeat seat(long showSeatId, long seatId, ShowSeatStatus status, Long holdId) {
        return new LockedSeat(showSeatId, seatId, "A", (int) seatId, "REGULAR", status, holdId, 20000);
    }

    @Test
    void createHold_allAvailable_holdsAndSnapshotsTotal() {
        when(bookingProperties.holdTtl()).thenReturn(Duration.ofMinutes(5));
        when(seatReservationService.lockSeats(1L, List.of(10L, 11L)))
                .thenReturn(List.of(seat(100, 10, ShowSeatStatus.AVAILABLE, null),
                        seat(101, 11, ShowSeatStatus.AVAILABLE, null)));
        when(seatHoldRepository.save(any(SeatHold.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldView view = seatHoldService.createHold(7L, 1L, List.of(10L, 11L));

        assertThat(view.hold().getTotalAmount()).isEqualTo(40000);
        assertThat(view.hold().getStatus()).isEqualTo(HoldStatus.ACTIVE);
        verify(seatReservationService).markHeld(List.of(100L, 101L), view.hold().getId());
    }

    @Test
    void createHold_seatBooked_throwsSeatUnavailable() {
        when(seatReservationService.lockSeats(1L, List.of(10L)))
                .thenReturn(List.of(seat(100, 10, ShowSeatStatus.BOOKED, null)));

        assertThatThrownBy(() -> seatHoldService.createHold(7L, 1L, List.of(10L)))
                .isInstanceOf(SeatUnavailableException.class);
        verify(seatReservationService, never()).markHeld(anyList(), any());
    }

    @Test
    void createHold_seatHeldByActiveHold_throwsSeatUnavailable() {
        SeatHold active = new SeatHold();
        active.setStatus(HoldStatus.ACTIVE);
        active.setExpiresAt(Instant.now().plus(Duration.ofMinutes(3)));
        when(seatReservationService.lockSeats(1L, List.of(10L)))
                .thenReturn(List.of(seat(100, 10, ShowSeatStatus.HELD, 5L)));
        when(seatHoldRepository.findById(5L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> seatHoldService.createHold(7L, 1L, List.of(10L)))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void createHold_seatHeldByExpiredHold_reclaims() {
        when(bookingProperties.holdTtl()).thenReturn(Duration.ofMinutes(5));
        SeatHold expired = new SeatHold();
        expired.setStatus(HoldStatus.ACTIVE);
        expired.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        when(seatReservationService.lockSeats(1L, List.of(10L)))
                .thenReturn(List.of(seat(100, 10, ShowSeatStatus.HELD, 5L)));
        when(seatHoldRepository.findById(5L)).thenReturn(Optional.of(expired));
        when(seatHoldRepository.save(any(SeatHold.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldView view = seatHoldService.createHold(7L, 1L, List.of(10L));

        assertThat(view.hold().getStatus()).isEqualTo(HoldStatus.ACTIVE);
        assertThat(expired.getStatus()).isEqualTo(HoldStatus.EXPIRED);
        verify(seatReservationService).releaseByHold(5L);
        verify(seatReservationService).markHeld(List.of(100L), view.hold().getId());
    }
}
