package com.moviebooking.ticket_booking.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end auth: register → login → authenticated access, plus RBAC and the seeded admin. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@moviebooking.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_thenLogin_thenAccessMe() throws Exception {
        String email = "cust-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane","email":"%s","password":"Secret123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("CUSTOMER")));

        String token = login(email, "Secret123");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        String body = """
                {"name":"Jane","email":"%s","password":"Secret123"}""".formatted(email);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("CONFLICT")));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}""".formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("INVALID_CREDENTIALS")));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHENTICATED")));
    }

    @Test
    void adminRoute_customerForbidden_adminAllowed() throws Exception {
        String customerEmail = "rbac-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cust","email":"%s","password":"Secret123"}""".formatted(customerEmail)))
                .andExpect(status().isCreated());

        String customerToken = login(customerEmail, "Secret123");
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/test/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(get("/api/v1/test/admin-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
