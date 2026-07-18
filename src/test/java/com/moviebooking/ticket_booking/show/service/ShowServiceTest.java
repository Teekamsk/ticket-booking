package com.moviebooking.ticket_booking.show.service;

import com.moviebooking.ticket_booking.catalog.api.CatalogQueryService;
import com.moviebooking.ticket_booking.catalog.api.MovieSummary;
import com.moviebooking.ticket_booking.catalog.api.ScreenLocation;
import com.moviebooking.ticket_booking.catalog.api.SeatView;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.refund.service.RefundPolicyService;
import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowSeat;
import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;
import com.moviebooking.ticket_booking.show.entity.ShowStatus;
import com.moviebooking.ticket_booking.show.repository.ShowRepository;
import com.moviebooking.ticket_booking.show.repository.ShowSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private CatalogQueryService catalogQueryService;
    @Mock
    private RefundPolicyService refundPolicyService;

    @InjectMocks
    private ShowService showService;

    private final Instant future = Instant.now().plus(Duration.ofDays(2));

    private void stubCatalog() {
        when(catalogQueryService.getActiveMovie(7L)).thenReturn(new MovieSummary(7L, "Dune", "English", "UA", 155));
        when(catalogQueryService.getActiveScreenLocation(5L))
                .thenReturn(new ScreenLocation(5L, "Audi 1", 3L, "PVR", 1L, "Bengaluru"));
        when(catalogQueryService.getActiveSeats(5L)).thenReturn(List.of(
                new SeatView(101L, "A", 1, "RECLINER"),
                new SeatView(102L, "B", 1, "REGULAR")));
    }

    private List<PriceItem> fullPricing() {
        return List.of(new PriceItem("RECLINER", 45000), new PriceItem("REGULAR", 20000));
    }

    @Test
    void create_happyPath_snapshotsCatalogAndGeneratesSeats() {
        stubCatalog();
        when(showRepository.overlaps(eq(5L), eq(ShowStatus.SCHEDULED), eq(0L), any(), any())).thenReturn(false);
        when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

        Show show = showService.create(7L, 5L, future, 9L, fullPricing());

        assertThat(show.getMovieTitle()).isEqualTo("Dune");
        assertThat(show.getCityName()).isEqualTo("Bengaluru");
        assertThat(show.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
        assertThat(show.getEndTime()).isEqualTo(future.plus(Duration.ofMinutes(155)));
        assertThat(show.getPricing()).hasSize(2);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ShowSeat>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(showSeatRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .allSatisfy(s -> assertThat(s.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE));
    }

    @Test
    void create_overlappingShow_throwsConflict() {
        stubCatalog();
        when(showRepository.overlaps(eq(5L), eq(ShowStatus.SCHEDULED), eq(0L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> showService.create(7L, 5L, future, 9L, fullPricing()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_pricingMissingSeatType_throwsBusinessRule() {
        stubCatalog();
        lenient().when(showRepository.overlaps(anyLong(), any(), anyLong(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> showService.create(7L, 5L, future, 9L, List.of(new PriceItem("RECLINER", 45000))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_startTimeInPast_throwsBusinessRule() {
        assertThatThrownBy(() -> showService.create(7L, 5L, Instant.now().minusSeconds(60), 9L, fullPricing()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_withHeldSeats_throwsBusinessRule() {
        Show show = new Show();
        show.setStatus(ShowStatus.SCHEDULED);
        when(showRepository.findById(1L)).thenReturn(java.util.Optional.of(show));
        when(showSeatRepository.existsByShowIdAndStatusNot(1L, ShowSeatStatus.AVAILABLE)).thenReturn(true);

        assertThatThrownBy(() -> showService.update(1L, future, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancel_scheduledShow_setsCancelled() {
        Show show = new Show();
        show.setStatus(ShowStatus.SCHEDULED);
        when(showRepository.findById(1L)).thenReturn(java.util.Optional.of(show));
        when(showSeatRepository.existsByShowIdAndStatus(1L, ShowSeatStatus.BOOKED)).thenReturn(false);

        showService.cancel(1L);

        assertThat(show.getStatus()).isEqualTo(ShowStatus.CANCELLED);
    }
}
