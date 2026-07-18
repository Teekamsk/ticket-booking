package com.moviebooking.ticket_booking.refund;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end refund-policy config: CRUD with rule replacement, RBAC, and conflict handling. */
@SpringBootTest
@AutoConfigureMockMvc
class RefundPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void create_get_update_replacesRuleSet() throws Exception {
        String admin = adminToken();
        String name = "Policy-" + System.nanoTime();

        Long id = ((Number) JsonPath.read(
                mockMvc.perform(authed(post("/api/v1/admin/refund-policies"), admin)
                                .content("{\"name\":\"%s\",\"rules\":[{\"minMinutesBeforeShow\":1440,\"refundPercent\":100},{\"minMinutesBeforeShow\":30,\"refundPercent\":0}]}".formatted(name)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.rules", hasSize(2)))
                        .andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(authed(get("/api/v1/admin/refund-policies/" + id), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(name)));

        // Replace rules, reusing threshold 1440 (must not clash with the removed rule).
        mockMvc.perform(authed(put("/api/v1/admin/refund-policies/" + id), admin)
                        .content("{\"name\":\"%s\",\"active\":true,\"rules\":[{\"minMinutesBeforeShow\":1440,\"refundPercent\":80},{\"minMinutesBeforeShow\":60,\"refundPercent\":25}]}".formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules", hasSize(2)))
                .andExpect(jsonPath("$.rules[0].minMinutesBeforeShow", is(60)))
                .andExpect(jsonPath("$.rules[1].refundPercent", is(80)));
    }

    @Test
    void create_duplicateName_409() throws Exception {
        String admin = adminToken();
        String name = "Dup-" + System.nanoTime();
        String body = "{\"name\":\"%s\",\"rules\":[{\"minMinutesBeforeShow\":30,\"refundPercent\":0}]}".formatted(name);

        mockMvc.perform(authed(post("/api/v1/admin/refund-policies"), admin).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(authed(post("/api/v1/admin/refund-policies"), admin).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("CONFLICT")));
    }

    @Test
    void create_duplicateThreshold_422() throws Exception {
        mockMvc.perform(authed(post("/api/v1/admin/refund-policies"), adminToken())
                        .content("{\"name\":\"T-%s\",\"rules\":[{\"minMinutesBeforeShow\":60,\"refundPercent\":50},{\"minMinutesBeforeShow\":60,\"refundPercent\":10}]}".formatted(System.nanoTime())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    void adminRoute_noToken401_customer403() throws Exception {
        String body = "{\"name\":\"X\",\"rules\":[{\"minMinutesBeforeShow\":30,\"refundPercent\":0}]}";
        mockMvc.perform(post("/api/v1/admin/refund-policies").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(authed(post("/api/v1/admin/refund-policies"), customerToken()).content(body))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String adminToken() throws Exception {
        return login("admin@moviebooking.com", "Admin@12345");
    }

    private String customerToken() throws Exception {
        String email = "refcust-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"C\",\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(email)))
                .andExpect(status().isCreated());
        return login(email, "Secret123");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
