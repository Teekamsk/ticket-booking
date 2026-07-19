package com.moviebooking.ticket_booking.discount;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.ticket_booking.discount.api.DiscountApplicationService;
import com.moviebooking.ticket_booking.discount.api.DiscountResult;
import com.moviebooking.ticket_booking.discount.exception.DiscountNotApplicableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end discount: admin CRUD + RBAC, plus the validate/compute/record/release cycle. */
@SpringBootTest
@AutoConfigureMockMvc
class DiscountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DiscountApplicationService applicationService;

    @Test
    void adminCrud_and_rbac() throws Exception {
        String admin = adminToken();
        String code = "CRUD" + System.nanoTime();
        long id = createDiscount(admin, code, "PERCENT", 15L, 0L, null);

        mockMvc.perform(authed(get("/api/v1/admin/discounts/" + id), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(code)));

        mockMvc.perform(authed(put("/api/v1/admin/discounts/" + id), admin)
                        .content(body(code, "PERCENT", 25, 0, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", is(25)));

        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/discounts/" + id), admin))
                .andExpect(status().isNoContent());

        // RBAC
        mockMvc.perform(post("/api/v1/admin/discounts").contentType(MediaType.APPLICATION_JSON)
                        .content(body("X", "FLAT", 5000, 0, null)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(authed(post("/api/v1/admin/discounts"), customerToken())
                        .content(body("Y" + System.nanoTime(), "FLAT", 5000, 0, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void validate_record_release_cycle() throws Exception {
        String admin = adminToken();
        String code = "CYC" + System.nanoTime();
        long id = createDiscount(admin, code, "PERCENT", 10L, 0L, 1); // maxUsesPerUser = 1

        DiscountResult result = applicationService.validateAndCompute(code, 1L, 100000);
        assertThat(result.discountAmount()).isEqualTo(10000);

        long bookingId = System.nanoTime();
        applicationService.recordRedemption(result.discountId(), 1L, bookingId, result.discountAmount());

        // Same user is now over the per-user limit
        assertThatThrownBy(() -> applicationService.validateAndCompute(code, 1L, 100000))
                .isInstanceOf(DiscountNotApplicableException.class);
        // A different user is still fine
        assertThat(applicationService.validateAndCompute(code, 2L, 100000).discountAmount()).isEqualTo(10000);

        // Releasing the redemption frees the use again
        applicationService.releaseRedemption(bookingId);
        assertThat(applicationService.validateAndCompute(code, 1L, 100000).discountAmount()).isEqualTo(10000);
    }

    // --- helpers ---

    private long createDiscount(String admin, String code, String type, long value, long minOrder, Integer perUser)
            throws Exception {
        String response = mockMvc.perform(authed(post("/api/v1/admin/discounts"), admin)
                        .content(body(code, type, value, minOrder, perUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String body(String code, String type, long value, long minOrder, Integer perUser) {
        String perUserJson = perUser == null ? "" : ",\"maxUsesPerUser\":" + perUser;
        return ("{\"code\":\"%s\",\"discountType\":\"%s\",\"value\":%d,\"minOrderAmount\":%d,"
                + "\"validFrom\":\"2026-01-01T00:00:00Z\",\"validTo\":\"2030-12-31T00:00:00Z\"%s}")
                .formatted(code, type, value, minOrder, perUserJson);
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String adminToken() throws Exception {
        return login("admin@moviebooking.com", "Admin@12345");
    }

    private String customerToken() throws Exception {
        String email = "disccust-" + System.nanoTime() + "@example.com";
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
