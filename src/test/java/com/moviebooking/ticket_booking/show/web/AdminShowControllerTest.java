package com.moviebooking.ticket_booking.show.web;

import com.moviebooking.ticket_booking.common.web.GlobalExceptionHandler;
import com.moviebooking.ticket_booking.show.dto.CreateShowRequest;
import com.moviebooking.ticket_booking.show.dto.ShowResponse;
import com.moviebooking.ticket_booking.show.handler.AdminShowRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminShowControllerTest {

    private AdminShowRequestHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = Mockito.mock(AdminShowRequestHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminShowController(handler))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(handler.create(any(CreateShowRequest.class)))
                .thenReturn(new ShowResponse(1L, 7L, "Dune", "English", "UA", 155,
                        5L, "Audi 1", 3L, "PVR", 1L, "Bengaluru",
                        Instant.now(), Instant.now(), "SCHEDULED", 9L, List.of()));
        String start = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mockMvc.perform(post("/api/v1/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":7,\"screenId\":5,\"startTime\":\"%s\",\"refundPolicyId\":9,\"prices\":[{\"seatType\":\"REGULAR\",\"price\":20000}]}".formatted(start)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    void create_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screenId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void create_emptyPrices_returns400() throws Exception {
        String start = Instant.now().plus(2, ChronoUnit.DAYS).toString();
        mockMvc.perform(post("/api/v1/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":7,\"screenId\":5,\"startTime\":\"%s\",\"refundPolicyId\":9,\"prices\":[]}".formatted(start)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }
}
