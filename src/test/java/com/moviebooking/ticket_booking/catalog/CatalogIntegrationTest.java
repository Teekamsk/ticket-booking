package com.moviebooking.ticket_booking.catalog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end catalog: admin CRUD chain, public browse, RBAC, validation, and soft-delete hiding. */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@moviebooking.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminBuildsCatalog_thenPublicCanBrowse() throws Exception {
        String admin = adminToken();
        String suffix = String.valueOf(System.nanoTime());
        String cityName = "City-" + suffix;
        String movieTitle = "Movie-" + suffix;

        Long cityId = extractId(post("/api/v1/admin/cities"), admin,
                "{\"name\":\"%s\",\"state\":\"Karnataka\"}".formatted(cityName));

        Long theatreId = extractId(post("/api/v1/admin/theatres"), admin,
                "{\"cityId\":%d,\"name\":\"T-%s\",\"address\":\"addr\"}".formatted(cityId, suffix));

        Long screenId = extractId(post("/api/v1/admin/screens"), admin,
                "{\"theatreId\":%d,\"name\":\"S-%s\"}".formatted(theatreId, suffix));

        mockMvc.perform(authed(post("/api/v1/admin/screens/" + screenId + "/seats"), admin)
                        .content("{\"rows\":[{\"rowLabel\":\"A\",\"seatType\":\"RECLINER\",\"count\":3},{\"rowLabel\":\"B\",\"seatType\":\"REGULAR\",\"count\":5}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount", is(8)))
                .andExpect(jsonPath("$.totalSeats", is(8)));

        mockMvc.perform(authed(post("/api/v1/admin/movies"), admin)
                        .content("{\"title\":\"%s\",\"language\":\"English\",\"genre\":\"SciFi\",\"durationMin\":148,\"certificate\":\"UA\",\"releaseDate\":\"2010-07-16\"}".formatted(movieTitle)))
                .andExpect(status().isCreated());

        // Public browse (no auth)
        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(cityName)));
        mockMvc.perform(get("/api/v1/movies").param("q", movieTitle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is(movieTitle)));
    }

    @Test
    void adminRoute_noToken401_customer403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cities").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"state\":\"Y\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(authed(post("/api/v1/admin/cities"), customerToken())
                        .content("{\"name\":\"X\",\"state\":\"Y\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    void createCity_invalidBody_400() throws Exception {
        mockMvc.perform(authed(post("/api/v1/admin/cities"), adminToken())
                        .content("{\"name\":\"\",\"state\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void createMovie_invalidCertificate_400Malformed() throws Exception {
        mockMvc.perform(authed(post("/api/v1/admin/movies"), adminToken())
                        .content("{\"title\":\"T\",\"language\":\"E\",\"genre\":\"G\",\"durationMin\":100,\"certificate\":\"XX\",\"releaseDate\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("MALFORMED_REQUEST")));
    }

    @Test
    void createTheatre_underMissingCity_404() throws Exception {
        mockMvc.perform(authed(post("/api/v1/admin/theatres"), adminToken())
                        .content("{\"cityId\":999999,\"name\":\"T\",\"address\":\"A\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void deactivatedCity_hiddenFromPublicBrowse() throws Exception {
        String admin = adminToken();
        String cityName = "Gone-" + System.nanoTime();
        Long cityId = extractId(post("/api/v1/admin/cities"), admin,
                "{\"name\":\"%s\",\"state\":\"KA\"}".formatted(cityName));

        mockMvc.perform(authed(delete("/api/v1/admin/cities/" + cityId), admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(cityName))));
    }

    // --- helpers ---

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private Long extractId(MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        String response = mockMvc.perform(authed(builder, token).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String customerToken() throws Exception {
        String email = "catcust-" + System.nanoTime() + "@example.com";
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
