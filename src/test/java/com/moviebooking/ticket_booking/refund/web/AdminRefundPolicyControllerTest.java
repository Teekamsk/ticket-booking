package com.moviebooking.ticket_booking.refund.web;

import com.moviebooking.ticket_booking.common.web.GlobalExceptionHandler;
import com.moviebooking.ticket_booking.refund.dto.RefundPolicyRequest;
import com.moviebooking.ticket_booking.refund.dto.RefundPolicyResponse;
import com.moviebooking.ticket_booking.refund.dto.RefundRuleResponse;
import com.moviebooking.ticket_booking.refund.handler.RefundPolicyRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminRefundPolicyControllerTest {

    private RefundPolicyRequestHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = Mockito.mock(RefundPolicyRequestHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRefundPolicyController(handler))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(handler.create(any(RefundPolicyRequest.class)))
                .thenReturn(new RefundPolicyResponse(1L, "Standard", true,
                        List.of(new RefundRuleResponse(1L, 30, 0))));

        mockMvc.perform(post("/api/v1/admin/refund-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Standard\",\"rules\":[{\"minMinutesBeforeShow\":30,\"refundPercent\":0}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.rules[0].refundPercent", is(0)));
    }

    @Test
    void create_emptyRules_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/refund-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Standard\",\"rules\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void create_percentOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/refund-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Standard\",\"rules\":[{\"minMinutesBeforeShow\":30,\"refundPercent\":150}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }
}
