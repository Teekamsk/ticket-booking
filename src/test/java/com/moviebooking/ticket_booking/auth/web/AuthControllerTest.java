package com.moviebooking.ticket_booking.auth.web;

import com.moviebooking.ticket_booking.auth.dto.AuthUserSummary;
import com.moviebooking.ticket_booking.auth.dto.LoginResponse;
import com.moviebooking.ticket_booking.auth.dto.RegisterRequest;
import com.moviebooking.ticket_booking.auth.dto.RegisterResponse;
import com.moviebooking.ticket_booking.auth.entity.Role;
import com.moviebooking.ticket_booking.auth.handler.AuthRequestHandler;
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

class AuthControllerTest {

    private AuthRequestHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        handler = Mockito.mock(AuthRequestHandler.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(handler))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_valid_returns201WithBody() throws Exception {
        when(handler.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "Jane", "jane@example.com", Role.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane","email":"jane@example.com","password":"Secret123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    void register_blankName_returns400WithFieldMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"jane@example.com","password":"Secret123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[0].field", is("name")));
    }

    @Test
    void login_valid_returns200WithToken() throws Exception {
        when(handler.login(any())).thenReturn(new LoginResponse("jwt-token", "Bearer", 3600,
                new AuthUserSummary(1L, "Jane", "jane@example.com", Role.CUSTOMER)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"Secret123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("jwt-token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }
}
