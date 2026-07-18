package com.moviebooking.ticket_booking.show.service;

import com.moviebooking.ticket_booking.catalog.api.CatalogQueryService;
import com.moviebooking.ticket_booking.catalog.api.MovieSummary;
import com.moviebooking.ticket_booking.catalog.api.ScreenLocation;
import com.moviebooking.ticket_booking.catalog.api.SeatView;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.refund.service.RefundPolicyService;
import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowPricing;
import com.moviebooking.ticket_booking.show.entity.ShowSeat;
import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;
import com.moviebooking.ticket_booking.show.entity.ShowStatus;
import com.moviebooking.ticket_booking.show.repository.ShowRepository;
import com.moviebooking.ticket_booking.show.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final CatalogQueryService catalogQueryService;
    private final RefundPolicyService refundPolicyService;

    @Transactional
    public Show create(Long movieId, Long screenId, Instant startTime, Long refundPolicyId, List<PriceItem> prices) {
        if (!startTime.isAfter(Instant.now())) {
            throw new BusinessRuleException("Show start time must be in the future");
        }
        refundPolicyService.requireActive(refundPolicyId);
        MovieSummary movie = catalogQueryService.getActiveMovie(movieId);
        ScreenLocation location = catalogQueryService.getActiveScreenLocation(screenId);
        List<SeatView> seats = catalogQueryService.getActiveSeats(screenId);
        if (seats.isEmpty()) {
            throw new BusinessRuleException("Screen " + screenId + " has no seats to schedule");
        }
        Instant endTime = startTime.plus(Duration.ofMinutes(movie.durationMin()));
        if (showRepository.overlaps(screenId, ShowStatus.SCHEDULED, 0L, startTime, endTime)) {
            throw new ConflictException("Screen already has a show scheduled in this time range");
        }
        validatePricingCoverage(prices, seatTypesOf(seats));

        Show show = new Show();
        show.setMovieId(movie.id());
        show.setMovieTitle(movie.title());
        show.setMovieLanguage(movie.language());
        show.setMovieCertificate(movie.certificate());
        show.setDurationMin(movie.durationMin());
        show.setScreenId(location.screenId());
        show.setScreenName(location.screenName());
        show.setTheatreId(location.theatreId());
        show.setTheatreName(location.theatreName());
        show.setCityId(location.cityId());
        show.setCityName(location.cityName());
        show.setStartTime(startTime);
        show.setEndTime(endTime);
        show.setStatus(ShowStatus.SCHEDULED);
        show.setRefundPolicyId(refundPolicyId);
        prices.forEach(price -> show.addPricing(pricing(price)));
        Show saved = showRepository.save(show);

        List<ShowSeat> showSeats = seats.stream().map(seat -> newShowSeat(saved, seat)).toList();
        showSeatRepository.saveAll(showSeats);
        log.info("Created show {} ({} @ {}) with {} seats", saved.getId(), movie.title(),
                location.screenName(), showSeats.size());
        return saved;
    }

    @Transactional
    public Show update(Long id, Instant newStartTime, List<PriceItem> newPrices) {
        Show show = getOrThrow(id);
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new BusinessRuleException("Only scheduled shows can be updated");
        }
        if (showSeatRepository.existsByShowIdAndStatusNot(id, ShowSeatStatus.AVAILABLE)) {
            throw new BusinessRuleException("Show has held or booked seats and cannot be updated");
        }
        if (newStartTime == null && newPrices == null) {
            throw new BusinessRuleException("Nothing to update: provide startTime and/or prices");
        }
        if (newStartTime != null) {
            if (!newStartTime.isAfter(Instant.now())) {
                throw new BusinessRuleException("Show start time must be in the future");
            }
            Instant endTime = newStartTime.plus(Duration.ofMinutes(show.getDurationMin()));
            if (showRepository.overlaps(show.getScreenId(), ShowStatus.SCHEDULED, id, newStartTime, endTime)) {
                throw new ConflictException("Screen already has a show scheduled in this time range");
            }
            show.setStartTime(newStartTime);
            show.setEndTime(endTime);
        }
        if (newPrices != null) {
            if (newPrices.isEmpty()) {
                throw new BusinessRuleException("Prices, if provided, must not be empty");
            }
            Set<String> seatTypes = show.getPricing().stream().map(ShowPricing::getSeatType)
                    .collect(java.util.stream.Collectors.toSet());
            validatePricingCoverage(newPrices, seatTypes);
            show.clearPricing();
            showRepository.flush();
            newPrices.forEach(price -> show.addPricing(pricing(price)));
        }
        return show;
    }

    @Transactional
    public Show cancel(Long id) {
        Show show = getOrThrow(id);
        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new BusinessRuleException("Show is already cancelled");
        }
        if (show.getStatus() == ShowStatus.COMPLETED) {
            throw new BusinessRuleException("A completed show cannot be cancelled");
        }
        if (showSeatRepository.existsByShowIdAndStatus(id, ShowSeatStatus.BOOKED)) {
            throw new BusinessRuleException("Show has booked seats; refund-driven cancellation is handled by the booking flow");
        }
        show.setStatus(ShowStatus.CANCELLED);
        log.info("Cancelled show {}", id);
        return show;
    }

    @Transactional(readOnly = true)
    public Show getOrThrow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + id + " not found"));
    }

    private void validatePricingCoverage(List<PriceItem> prices, Set<String> seatTypes) {
        Set<String> priced = new HashSet<>();
        for (PriceItem price : prices) {
            if (!priced.add(price.seatType())) {
                throw new BusinessRuleException("Duplicate price for seat type " + price.seatType());
            }
        }
        if (!priced.equals(seatTypes)) {
            throw new BusinessRuleException("Pricing must cover exactly the seat types present: " + seatTypes);
        }
    }

    private Set<String> seatTypesOf(List<SeatView> seats) {
        return seats.stream().map(SeatView::seatType).collect(java.util.stream.Collectors.toSet());
    }

    private ShowPricing pricing(PriceItem item) {
        ShowPricing pricing = new ShowPricing();
        pricing.setSeatType(item.seatType());
        pricing.setPrice(item.price());
        return pricing;
    }

    private ShowSeat newShowSeat(Show show, SeatView seat) {
        ShowSeat showSeat = new ShowSeat();
        showSeat.setShow(show);
        showSeat.setSeatId(seat.seatId());
        showSeat.setRowLabel(seat.rowLabel());
        showSeat.setSeatNumber(seat.seatNumber());
        showSeat.setSeatType(seat.seatType());
        showSeat.setStatus(ShowSeatStatus.AVAILABLE);
        return showSeat;
    }
}
