package com.moviebooking.ticket_booking.catalog.handler;

import com.moviebooking.ticket_booking.catalog.dto.CreateScreenRequest;
import com.moviebooking.ticket_booking.catalog.dto.CreateSeatsRequest;
import com.moviebooking.ticket_booking.catalog.dto.CreateSeatsResponse;
import com.moviebooking.ticket_booking.catalog.dto.ScreenResponse;
import com.moviebooking.ticket_booking.catalog.dto.SeatResponse;
import com.moviebooking.ticket_booking.catalog.dto.UpdateScreenRequest;
import com.moviebooking.ticket_booking.catalog.entity.Seat;
import com.moviebooking.ticket_booking.catalog.mapper.CatalogMapper;
import com.moviebooking.ticket_booking.catalog.service.ScreenService;
import com.moviebooking.ticket_booking.catalog.service.SeatRowSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScreenRequestHandler {

    private final ScreenService screenService;

    public ScreenResponse create(CreateScreenRequest request) {
        return CatalogMapper.toScreenResponse(screenService.create(request.theatreId(), request.name()));
    }

    public ScreenResponse update(Long id, UpdateScreenRequest request) {
        return CatalogMapper.toScreenResponse(screenService.update(id, request.name()));
    }

    public CreateSeatsResponse addSeats(Long screenId, CreateSeatsRequest request) {
        List<SeatRowSpec> specs = request.rows().stream()
                .map(row -> new SeatRowSpec(row.rowLabel(), row.seatType(), row.count()))
                .toList();
        List<Seat> created = screenService.addSeats(screenId, specs);
        List<SeatResponse> seats = created.stream().map(CatalogMapper::toSeatResponse).toList();
        int totalSeats = created.get(0).getScreen().getTotalSeats();
        return new CreateSeatsResponse(screenId, seats.size(), totalSeats, seats);
    }

    public List<SeatResponse> listSeats(Long screenId) {
        return screenService.listSeats(screenId).stream().map(CatalogMapper::toSeatResponse).toList();
    }
}
