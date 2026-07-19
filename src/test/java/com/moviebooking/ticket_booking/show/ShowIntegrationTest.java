package com.moviebooking.ticket_booking.show;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end show module: creation (denormalized), read endpoints, update/cancel, and guards. */
@SpringBootTest
@AutoConfigureMockMvc
class ShowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private record Ctx(long cityId, long screenId, long movieId, long policyId) {
    }

    @Test
    void create_detail_seatMap_search_update_cancel() throws Exception {
        String admin = adminToken();
        Ctx ctx = setup(admin);
        String start = futureIso(2);

        String showJson = mockMvc.perform(authed(post("/api/v1/admin/shows"), admin)
                        .content(createShowBody(ctx, start)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieTitle", is("Interstellar")))
                .andExpect(jsonPath("$.cityName", org.hamcrest.Matchers.startsWith("ShowCity")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andReturn().getResponse().getContentAsString();
        long showId = ((Number) JsonPath.read(showJson, "$.id")).longValue();

        // Public detail (no auth)
        mockMvc.perform(get("/api/v1/shows/" + showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screenName", is("Audi 1")))
                .andExpect(jsonPath("$.prices", org.hamcrest.Matchers.hasSize(2)));

        // Seat map: 5 seats (2 RECLINER + 3 REGULAR), all AVAILABLE
        mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$.seats[0].status", is("AVAILABLE")));

        // Public search grouped by theatre
        mockMvc.perform(get("/api/v1/shows").param("cityId", String.valueOf(ctx.cityId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].theatreName", org.hamcrest.Matchers.startsWith("ShowPlex")))
                .andExpect(jsonPath("$[0].shows[0].id", is((int) showId)));

        // Update prices (all seats still AVAILABLE)
        mockMvc.perform(authed(put("/api/v1/admin/shows/" + showId), admin)
                        .content("{\"prices\":[{\"seatType\":\"RECLINER\",\"price\":50000},{\"seatType\":\"REGULAR\",\"price\":25000}]}"))
                .andExpect(status().isOk());

        // Cancel
        mockMvc.perform(authed(post("/api/v1/admin/shows/" + showId + "/cancel"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void create_overlappingOnSameScreen_409() throws Exception {
        String admin = adminToken();
        Ctx ctx = setup(admin);
        String start = futureIso(3);
        mockMvc.perform(authed(post("/api/v1/admin/shows"), admin).content(createShowBody(ctx, start)))
                .andExpect(status().isCreated());
        mockMvc.perform(authed(post("/api/v1/admin/shows"), admin).content(createShowBody(ctx, start)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_missingSeatTypePrice_422() throws Exception {
        String admin = adminToken();
        Ctx ctx = setup(admin);
        mockMvc.perform(authed(post("/api/v1/admin/shows"), admin)
                        .content("{\"movieId\":%d,\"screenId\":%d,\"startTime\":\"%s\",\"refundPolicyId\":%d,\"prices\":[{\"seatType\":\"RECLINER\",\"price\":45000}]}"
                                .formatted(ctx.movieId(), ctx.screenId(), futureIso(4), ctx.policyId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    void create_pastStartTime_400() throws Exception {
        String admin = adminToken();
        Ctx ctx = setup(admin);
        String past = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        mockMvc.perform(authed(post("/api/v1/admin/shows"), admin).content(createShowBody(ctx, past)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminRoute_noToken401_customer403() throws Exception {
        Ctx ctx = setup(adminToken());
        String body = createShowBody(ctx, futureIso(5));
        mockMvc.perform(post("/api/v1/admin/shows").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(authed(post("/api/v1/admin/shows"), customerToken()).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void search_missingCityId_400() throws Exception {
        mockMvc.perform(get("/api/v1/shows"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("MISSING_PARAMETER")));
    }

    // --- helpers ---

    private Ctx setup(String admin) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long cityId = createId("/api/v1/admin/cities", admin,
                "{\"name\":\"ShowCity\",\"state\":\"KA\"}".replace("ShowCity", "ShowCity-" + suffix));
        long theatreId = createId("/api/v1/admin/theatres", admin,
                "{\"cityId\":%d,\"name\":\"ShowPlex-%s\",\"address\":\"MG Road\"}".formatted(cityId, suffix));
        long screenId = createId("/api/v1/admin/screens", admin,
                "{\"theatreId\":%d,\"name\":\"Audi 1\"}".formatted(theatreId));
        mockMvc.perform(authed(post("/api/v1/admin/screens/" + screenId + "/seats"), admin)
                        .content("{\"rows\":[{\"rowLabel\":\"A\",\"seatType\":\"RECLINER\",\"count\":2},{\"rowLabel\":\"B\",\"seatType\":\"REGULAR\",\"count\":3}]}"))
                .andExpect(status().isCreated());
        long movieId = createId("/api/v1/admin/movies", admin,
                "{\"title\":\"Interstellar\",\"language\":\"English\",\"genre\":\"SciFi\",\"durationMin\":169,\"certificate\":\"UA\",\"releaseDate\":\"2014-11-07\"}");
        long policyId = createId("/api/v1/admin/refund-policies", admin,
                "{\"name\":\"ShowStd-%s\",\"rules\":[{\"minMinutesBeforeShow\":120,\"refundPercent\":50},{\"minMinutesBeforeShow\":30,\"refundPercent\":0}]}".formatted(suffix));
        return new Ctx(cityId, screenId, movieId, policyId);
    }

    private String createShowBody(Ctx ctx, String startIso) {
        return "{\"movieId\":%d,\"screenId\":%d,\"startTime\":\"%s\",\"refundPolicyId\":%d,\"prices\":[{\"seatType\":\"RECLINER\",\"price\":45000},{\"seatType\":\"REGULAR\",\"price\":20000}]}"
                .formatted(ctx.movieId(), ctx.screenId(), startIso, ctx.policyId());
    }

    private String futureIso(int days) {
        return Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private long createId(String path, String token, String body) throws Exception {
        String response = mockMvc.perform(authed(post(path), token).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String adminToken() throws Exception {
        return login("admin@moviebooking.com", "Admin@12345");
    }

    private String customerToken() throws Exception {
        String email = "showcust-" + System.nanoTime() + "@example.com";
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
