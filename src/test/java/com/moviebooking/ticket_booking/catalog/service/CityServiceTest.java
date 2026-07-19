package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.entity.City;
import com.moviebooking.ticket_booking.catalog.repository.CityRepository;
import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    @Test
    void create_uniqueName_savesActiveCity() {
        when(cityRepository.existsByNameIgnoreCase("Pune")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenAnswer(inv -> inv.getArgument(0));

        City result = cityService.create("Pune", "Maharashtra");

        assertThat(result.getName()).isEqualTo("Pune");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(cityRepository.existsByNameIgnoreCase("Pune")).thenReturn(true);

        assertThatThrownBy(() -> cityService.create("Pune", "Maharashtra"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_toAnotherExistingName_throwsConflict() {
        City existing = new City();
        existing.setName("Pune");
        when(cityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cityRepository.existsByNameIgnoreCase("Mumbai")).thenReturn(true);

        assertThatThrownBy(() -> cityService.update(1L, "Mumbai", "Maharashtra"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_sameNameDifferentState_succeeds() {
        City existing = new City();
        existing.setName("Pune");
        existing.setState("MH");
        when(cityRepository.findById(1L)).thenReturn(Optional.of(existing));

        City result = cityService.update(1L, "Pune", "Maharashtra");

        assertThat(result.getState()).isEqualTo("Maharashtra");
    }

    @Test
    void getActiveOrThrow_inactiveCity_throwsBusinessRule() {
        City inactive = new City();
        inactive.setActive(false);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> cityService.getActiveOrThrow(1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getOrThrow_missing_throwsNotFound() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getOrThrow(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
