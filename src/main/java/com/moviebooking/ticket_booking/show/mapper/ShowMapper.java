package com.moviebooking.ticket_booking.show.mapper;

import com.moviebooking.ticket_booking.show.dto.SeatMapEntry;
import com.moviebooking.ticket_booking.show.dto.SeatMapResponse;
import com.moviebooking.ticket_booking.show.dto.ShowPricingResponse;
import com.moviebooking.ticket_booking.show.dto.ShowResponse;
import com.moviebooking.ticket_booking.show.dto.ShowSummaryResponse;
import com.moviebooking.ticket_booking.show.dto.TheatreShowsResponse;
import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowPricing;
import com.moviebooking.ticket_booking.show.service.SeatMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Entity → response DTO mapping for shows. Keeps entities out of the API. */
public final class ShowMapper {

    private ShowMapper() {
    }

    public static ShowResponse toShowResponse(Show show) {
        List<ShowPricingResponse> prices = show.getPricing().stream()
                .sorted(Comparator.comparing(ShowPricing::getSeatType))
                .map(p -> new ShowPricingResponse(p.getSeatType(), p.getPrice()))
                .toList();
        return new ShowResponse(show.getId(),
                show.getMovieId(), show.getMovieTitle(), show.getMovieLanguage(), show.getMovieCertificate(),
                show.getDurationMin(),
                show.getScreenId(), show.getScreenName(), show.getTheatreId(), show.getTheatreName(),
                show.getCityId(), show.getCityName(),
                show.getStartTime(), show.getEndTime(), show.getStatus().name(), show.getRefundPolicyId(), prices);
    }

    public static List<TheatreShowsResponse> groupByTheatre(List<Show> shows) {
        Map<Long, List<Show>> byTheatre = new LinkedHashMap<>();
        for (Show show : shows) {
            byTheatre.computeIfAbsent(show.getTheatreId(), k -> new ArrayList<>()).add(show);
        }
        return byTheatre.values().stream().map(group -> {
            Show first = group.get(0);
            List<ShowSummaryResponse> summaries = group.stream().map(ShowMapper::toSummary).toList();
            return new TheatreShowsResponse(first.getTheatreId(), first.getTheatreName(),
                    first.getCityId(), first.getCityName(), summaries);
        }).toList();
    }

    public static SeatMapResponse toSeatMapResponse(SeatMap map) {
        Show show = map.show();
        Map<String, Long> priceByType = show.getPricing().stream()
                .collect(Collectors.toMap(ShowPricing::getSeatType, ShowPricing::getPrice));
        List<SeatMapEntry> entries = map.seats().stream()
                .map(seat -> new SeatMapEntry(seat.getId(), seat.getSeatId(), seat.getRowLabel(),
                        seat.getSeatNumber(), seat.getSeatType(), seat.getStatus().name(),
                        priceByType.getOrDefault(seat.getSeatType(), 0L)))
                .toList();
        return new SeatMapResponse(show.getId(), show.getMovieTitle(), show.getScreenName(),
                show.getTheatreName(), show.getStartTime(), entries);
    }

    private static ShowSummaryResponse toSummary(Show show) {
        return new ShowSummaryResponse(show.getId(), show.getMovieId(), show.getMovieTitle(),
                show.getMovieCertificate(), show.getMovieLanguage(), show.getDurationMin(),
                show.getScreenId(), show.getScreenName(), show.getStartTime(), show.getEndTime(),
                show.getStatus().name());
    }
}
