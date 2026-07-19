package com.moviebooking.ticket_booking.payment.web;

import com.moviebooking.ticket_booking.common.web.GlobalExceptionHandler;
import com.moviebooking.ticket_booking.payment.handler.PaymentRequestHandler;
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

class PaymentControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(Mockito.mock(PaymentRequestHandler.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pay_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"UPI\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void pay_invalidMethod_returns400Malformed() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":1,\"method\":\"BITCOIN\",\"idempotencyKey\":\"k\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("MALFORMED_REQUEST")));
    }
}
