package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.City;
import com.moviebooking.ticket_booking.catalog.entity.Theatre;
import com.moviebooking.ticket_booking.catalog.repository.TheatreRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final CityService cityService;

    @Transactional
    public Theatre create(Long cityId, String name, String address) {
        City city = cityService.getActiveOrThrow(cityId);
        if (theatreRepository.existsByCityIdAndNameIgnoreCase(cityId, name)) {
            throw new ConflictException("Theatre '" + name + "' already exists in this city");
        }
        Theatre theatre = new Theatre();
        theatre.setCity(city);
        theatre.setName(name);
        theatre.setAddress(address);
        theatre.setActive(true);
        Theatre saved = theatreRepository.save(theatre);
        log.info("Created theatre {} (id={}) in city {}", saved.getName(), saved.getId(), cityId);
        return saved;
    }

    @Transactional
    public Theatre update(Long id, String name, String address) {
        Theatre theatre = getOrThrow(id);
        if (!theatre.getName().equalsIgnoreCase(name)
                && theatreRepository.existsByCityIdAndNameIgnoreCase(theatre.getCity().getId(), name)) {
            throw new ConflictException("Theatre '" + name + "' already exists in this city");
        }
        theatre.setName(name);
        theatre.setAddress(address);
        return theatre;
    }

    @Transactional
    public void deactivate(Long id) {
        Theatre theatre = getOrThrow(id);
        theatre.setActive(false);
        log.info("Deactivated theatre id={}", id);
    }

    public Theatre getOrThrow(Long id) {
        return theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre " + id + " not found"));
    }

    /** For child creation: the parent theatre must exist and be active. */
    public Theatre getActiveOrThrow(Long id) {
        Theatre theatre = getOrThrow(id);
        if (!theatre.isActive()) {
            throw new BusinessRuleException("Theatre " + id + " is not active");
        }
        return theatre;
    }
}
