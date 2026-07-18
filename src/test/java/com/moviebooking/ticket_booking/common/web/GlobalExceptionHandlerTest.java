package com.moviebooking.ticket_booking.common.web;

import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void apiException_mapsToProblemDetailWithStatusAndErrorCode() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", is("City 99 not found")))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void dataIntegrityViolation_mapsToConflict() throws Exception {
        mockMvc.perform(get("/test/data-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("DATA_CONFLICT")));
    }

    @Test
    void validationError_mapsToBadRequestWithFieldMessages() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[0].field", is("name")))
                .andExpect(jsonPath("$.errors[0].message", is("name is required")));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @org.springframework.web.bind.annotation.GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("City 99 not found");
        }

        @org.springframework.web.bind.annotation.GetMapping("/data-conflict")
        String dataConflict() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody SampleRequest request) {
            return "ok";
        }
    }

    record SampleRequest(@NotBlank(message = "name is required") String name) {
    }
}
