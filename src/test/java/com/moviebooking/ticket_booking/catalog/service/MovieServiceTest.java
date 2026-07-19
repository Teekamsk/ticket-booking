package com.moviebooking.ticket_booking.catalog.service;

import com.moviebooking.ticket_booking.catalog.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    @Test
    void browse_withQuery_delegatesToTitleSearch() {
        when(movieRepository.findByActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc("bat"))
                .thenReturn(List.of());

        movieService.browse("  bat  ");

        verify(movieRepository).findByActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc("bat");
        verifyNoMoreInteractions(movieRepository);
    }

    @Test
    void browse_blankQuery_listsAllActive() {
        when(movieRepository.findByActiveTrueOrderByTitleAsc()).thenReturn(List.of());

        movieService.browse("   ");

        verify(movieRepository).findByActiveTrueOrderByTitleAsc();
        verifyNoMoreInteractions(movieRepository);
    }
}
