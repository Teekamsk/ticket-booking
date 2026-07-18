package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.City;
import com.moviebooking.ticket_booking.catalog.repository.CityRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional
    public City create(String name, String state) {
        if (cityRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("City '" + name + "' already exists");
        }
        City city = new City();
        city.setName(name);
        city.setState(state);
        city.setActive(true);
        City saved = cityRepository.save(city);
        log.info("Created city {} (id={})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public City update(Long id, String name, String state) {
        City city = getOrThrow(id);
        if (!city.getName().equalsIgnoreCase(name) && cityRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("City '" + name + "' already exists");
        }
        city.setName(name);
        city.setState(state);
        return city;
    }

    @Transactional
    public void deactivate(Long id) {
        City city = getOrThrow(id);
        city.setActive(false);
        log.info("Deactivated city id={}", id);
    }

    @Transactional(readOnly = true)
    public List<City> listActive() {
        return cityRepository.findByActiveTrueOrderByNameAsc();
    }

    public City getOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City " + id + " not found"));
    }

    /** For child creation: the parent city must exist and be active. */
    public City getActiveOrThrow(Long id) {
        City city = getOrThrow(id);
        if (!city.isActive()) {
            throw new BusinessRuleException("City " + id + " is not active");
        }
        return city;
    }
}
