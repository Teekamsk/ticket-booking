package com.moviebooking.ticket_booking.booking.web;

import com.moviebooking.ticket_booking.booking.handler.HoldRequestHandler;
import com.moviebooking.ticket_booking.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Validation happens before the controller body runs, so no authenticated principal is needed here. */
class HoldControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HoldController(Mockito.mock(HoldRequestHandler.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_emptySeatIds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":1,\"seatIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void create_missingShowId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }
}
