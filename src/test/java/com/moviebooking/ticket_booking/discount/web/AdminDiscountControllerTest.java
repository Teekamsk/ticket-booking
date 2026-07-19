package com.moviebooking.ticket_booking.discount.web;

import com.moviebooking.ticket_booking.discount.dto.DiscountRequest;
import com.moviebooking.ticket_booking.discount.dto.DiscountResponse;
import com.moviebooking.ticket_booking.discount.entity.DiscountType;
import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;
import com.moviebooking.ticket_booking.discount.handler.DiscountRequestHandler;
import com.moviebooking.ticket_booking.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminDiscountControllerTest {

    private DiscountRequestHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = Mockito.mock(DiscountRequestHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDiscountController(handler))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(handler.create(any(DiscountRequest.class))).thenReturn(new DiscountResponse(
                1L, DiscountType.PROMO, "SAVE20", DiscountValueType.PERCENT, 20, 10000L, 50000,
                Instant.now(), Instant.now(), 100, 2, true));

        mockMvc.perform(post("/api/v1/admin/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE20\",\"discountType\":\"PERCENT\",\"value\":20,\"minOrderAmount\":50000,\"validFrom\":\"2026-01-01T00:00:00Z\",\"validTo\":\"2026-12-31T00:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("SAVE20")))
                .andExpect(jsonPath("$.type", is("PROMO")));
    }

    @Test
    void create_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"discountType\":\"PERCENT\",\"value\":20,\"minOrderAmount\":0,\"validFrom\":\"2026-01-01T00:00:00Z\",\"validTo\":\"2026-12-31T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void create_negativeValue_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"discountType\":\"FLAT\",\"value\":-5,\"minOrderAmount\":0,\"validFrom\":\"2026-01-01T00:00:00Z\",\"validTo\":\"2026-12-31T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }
}
