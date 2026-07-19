package com.moviebooking.ticket_booking.show.handler;

import com.moviebooking.ticket_booking.show.dto.CreateShowRequest;
import com.moviebooking.ticket_booking.show.dto.ShowResponse;
import com.moviebooking.ticket_booking.show.dto.UpdateShowRequest;
import com.moviebooking.ticket_booking.show.mapper.ShowMapper;
import com.moviebooking.ticket_booking.show.service.PriceItem;
import com.moviebooking.ticket_booking.show.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminShowRequestHandler {

    private final ShowService showService;

    public ShowResponse create(CreateShowRequest request) {
        List<PriceItem> prices = request.prices().stream()
                .map(p -> new PriceItem(p.seatType(), p.price())).toList();
        return ShowMapper.toShowResponse(showService.create(
                request.movieId(), request.screenId(), request.startTime(), request.refundPolicyId(), prices));
    }

    public ShowResponse update(Long id, UpdateShowRequest request) {
        List<PriceItem> prices = request.prices() == null ? null
                : request.prices().stream().map(p -> new PriceItem(p.seatType(), p.price())).toList();
        return ShowMapper.toShowResponse(showService.update(id, request.startTime(), prices));
    }

    public ShowResponse cancel(Long id) {
        return ShowMapper.toShowResponse(showService.cancel(id));
    }
}
