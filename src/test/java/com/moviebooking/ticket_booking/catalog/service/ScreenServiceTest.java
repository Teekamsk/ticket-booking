package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.Screen;
import com.moviebooking.ticket_booking.catalog.entity.Seat;
import com.moviebooking.ticket_booking.catalog.entity.SeatType;
import com.moviebooking.ticket_booking.catalog.repository.ScreenRepository;
import com.moviebooking.ticket_booking.catalog.repository.SeatRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenServiceTest {

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private TheatreService theatreService;

    @InjectMocks
    private ScreenService screenService;

    private Screen activeScreen() {
        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setTotalSeats(0);
        screen.setActive(true);
        return screen;
    }

    @Test
    void addSeats_generatesSeatsAndUpdatesTotal() {
        Screen screen = activeScreen();
        when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
        when(seatRepository.existsByScreenIdAndRowLabelIgnoreCase(anyLong(), any())).thenReturn(false);
        when(seatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Seat> result = screenService.addSeats(1L, List.of(
                new SeatRowSpec("A", SeatType.RECLINER, 2),
                new SeatRowSpec("B", SeatType.REGULAR, 3)));

        assertThat(result).hasSize(5);
        assertThat(screen.getTotalSeats()).isEqualTo(5);
        assertThat(result.get(0).getRowLabel()).isEqualTo("A");
        assertThat(result.get(0).getSeatNumber()).isEqualTo(1);
        assertThat(result.get(0).getSeatType()).isEqualTo(SeatType.RECLINER);
    }

    @Test
    void addSeats_duplicateRowInRequest_throwsBusinessRule() {
        Screen screen = activeScreen();
        when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
        lenient().when(seatRepository.existsByScreenIdAndRowLabelIgnoreCase(anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> screenService.addSeats(1L, List.of(
                new SeatRowSpec("A", SeatType.REGULAR, 2),
                new SeatRowSpec("a", SeatType.REGULAR, 2))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addSeats_existingRow_throwsConflict() {
        Screen screen = activeScreen();
        when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
        when(seatRepository.existsByScreenIdAndRowLabelIgnoreCase(eq(1L), eq("A"))).thenReturn(true);

        assertThatThrownBy(() -> screenService.addSeats(1L, List.of(new SeatRowSpec("A", SeatType.REGULAR, 2))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(theatreService.getActiveOrThrow(1L)).thenReturn(null);
        when(screenRepository.existsByTheatreIdAndNameIgnoreCase(1L, "Screen 1")).thenReturn(true);

        assertThatThrownBy(() -> screenService.create(1L, "Screen 1"))
                .isInstanceOf(ConflictException.class);
    }
}
