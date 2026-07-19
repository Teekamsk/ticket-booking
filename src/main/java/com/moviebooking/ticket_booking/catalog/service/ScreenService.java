package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.Screen;
import com.moviebooking.ticket_booking.catalog.entity.Seat;
import com.moviebooking.ticket_booking.catalog.entity.Theatre;
import com.moviebooking.ticket_booking.catalog.repository.ScreenRepository;
import com.moviebooking.ticket_booking.catalog.repository.SeatRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final TheatreService theatreService;

    @Transactional
    public Screen create(Long theatreId, String name) {
        Theatre theatre = theatreService.getActiveOrThrow(theatreId);
        if (screenRepository.existsByTheatreIdAndNameIgnoreCase(theatreId, name)) {
            throw new ConflictException("Screen '" + name + "' already exists in this theatre");
        }
        Screen screen = new Screen();
        screen.setTheatre(theatre);
        screen.setName(name);
        screen.setTotalSeats(0);
        screen.setActive(true);
        Screen saved = screenRepository.save(screen);
        log.info("Created screen {} (id={}) in theatre {}", saved.getName(), saved.getId(), theatreId);
        return saved;
    }

    @Transactional
    public Screen update(Long id, String name) {
        Screen screen = getOrThrow(id);
        if (!screen.getName().equalsIgnoreCase(name)
                && screenRepository.existsByTheatreIdAndNameIgnoreCase(screen.getTheatre().getId(), name)) {
            throw new ConflictException("Screen '" + name + "' already exists in this theatre");
        }
        screen.setName(name);
        return screen;
    }

    /** Generates seats for one or more rows and updates the screen's total. Fails atomically on any conflict. */
    @Transactional
    public List<Seat> addSeats(Long screenId, List<SeatRowSpec> rows) {
        Screen screen = getActiveOrThrow(screenId);
        Set<String> requestedRows = new HashSet<>();
        List<Seat> toCreate = new ArrayList<>();
        for (SeatRowSpec row : rows) {
            String rowLabel = row.rowLabel().trim().toUpperCase();
            if (!requestedRows.add(rowLabel)) {
                throw new BusinessRuleException("Row '" + rowLabel + "' is duplicated in the request");
            }
            if (seatRepository.existsByScreenIdAndRowLabelIgnoreCase(screenId, rowLabel)) {
                throw new ConflictException("Row '" + rowLabel + "' already has seats on this screen");
            }
            for (int number = 1; number <= row.count(); number++) {
                Seat seat = new Seat();
                seat.setScreen(screen);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(number);
                seat.setSeatType(row.seatType());
                seat.setActive(true);
                toCreate.add(seat);
            }
        }
        List<Seat> saved = seatRepository.saveAll(toCreate);
        screen.setTotalSeats(screen.getTotalSeats() + saved.size());
        log.info("Added {} seats to screen {}", saved.size(), screenId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Seat> listSeats(Long screenId) {
        getOrThrow(screenId);
        return seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId);
    }

    public Screen getOrThrow(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen " + id + " not found"));
    }

    public Screen getActiveOrThrow(Long id) {
        Screen screen = getOrThrow(id);
        if (!screen.isActive()) {
            throw new BusinessRuleException("Screen " + id + " is not active");
        }
        return screen;
    }
}
