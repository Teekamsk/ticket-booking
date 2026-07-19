package com.moviebooking.ticket_booking.catalog.web;

import com.moviebooking.ticket_booking.catalog.dto.CityRequest;
import com.moviebooking.ticket_booking.catalog.dto.CityResponse;
import com.moviebooking.ticket_booking.catalog.handler.CityRequestHandler;
import com.moviebooking.ticket_booking.catalog.handler.MovieRequestHandler;
import com.moviebooking.ticket_booking.catalog.handler.ScreenRequestHandler;
import com.moviebooking.ticket_booking.catalog.handler.TheatreRequestHandler;
import com.moviebooking.ticket_booking.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCatalogControllerTest {

    private CityRequestHandler cityHandler;
    private ScreenRequestHandler screenHandler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cityHandler = Mockito.mock(CityRequestHandler.class);
        screenHandler = Mockito.mock(ScreenRequestHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCatalogController(
                        cityHandler, Mockito.mock(TheatreRequestHandler.class),
                        screenHandler, Mockito.mock(MovieRequestHandler.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCity_valid_returns201() throws Exception {
        when(cityHandler.create(any(CityRequest.class)))
                .thenReturn(new CityResponse(1L, "Pune", "Maharashtra", true));

        mockMvc.perform(post("/api/v1/admin/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pune\",\"state\":\"Maharashtra\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createCity_blankFields_returns400WithMessages() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"state\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void addSeats_emptyRows_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/screens/1/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }
}
