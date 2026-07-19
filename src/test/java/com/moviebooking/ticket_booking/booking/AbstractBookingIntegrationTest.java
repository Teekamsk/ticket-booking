package com.moviebooking.ticket_booking.booking;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared setup for booking integration tests: builds a show with seats and mints tokens. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractBookingIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected record ShowFixture(long showId, List<Long> seatIds, long policyId) {
    }

    /** Admin-creates city→theatre→screen→seats→movie→policy→show and returns its seat ids. */
    protected ShowFixture createShow(int seatCount) throws Exception {
        return createShow(seatCount, Instant.now().plus(2, ChronoUnit.DAYS));
    }

    protected ShowFixture createShow(int seatCount, Instant startTime) throws Exception {
        String admin = adminToken();
        String suffix = String.valueOf(System.nanoTime());
        long cityId = createId(admin, "/api/v1/admin/cities",
                "{\"name\":\"C-%s\",\"state\":\"KA\"}".formatted(suffix));
        long theatreId = createId(admin, "/api/v1/admin/theatres",
                "{\"cityId\":%d,\"name\":\"T-%s\",\"address\":\"x\"}".formatted(cityId, suffix));
        long screenId = createId(admin, "/api/v1/admin/screens",
                "{\"theatreId\":%d,\"name\":\"S1\"}".formatted(theatreId));
        mockMvc.perform(authed(post("/api/v1/admin/screens/" + screenId + "/seats"), admin)
                        .content("{\"rows\":[{\"rowLabel\":\"A\",\"seatType\":\"REGULAR\",\"count\":%d}]}".formatted(seatCount)))
                .andExpect(status().isCreated());
        long movieId = createId(admin, "/api/v1/admin/movies",
                "{\"title\":\"M-%s\",\"language\":\"EN\",\"genre\":\"X\",\"durationMin\":120,\"certificate\":\"UA\",\"releaseDate\":\"2020-01-01\"}".formatted(suffix));
        long policyId = createId(admin, "/api/v1/admin/refund-policies",
                "{\"name\":\"P-%s\",\"rules\":[{\"minMinutesBeforeShow\":120,\"refundPercent\":50},{\"minMinutesBeforeShow\":30,\"refundPercent\":0}]}".formatted(suffix));
        String start = startTime.truncatedTo(ChronoUnit.MINUTES).toString();
        long showId = createId(admin, "/api/v1/admin/shows",
                "{\"movieId\":%d,\"screenId\":%d,\"startTime\":\"%s\",\"refundPolicyId\":%d,\"prices\":[{\"seatType\":\"REGULAR\",\"price\":20000}]}"
                        .formatted(movieId, screenId, start, policyId));
        String seatsJson = mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(seatsJson, "$.seats[*].seatId");
        return new ShowFixture(showId, ids.stream().map(Integer::longValue).toList(), policyId);
    }

    protected String holdSeats(String token, long showId, List<Long> seatIds) throws Exception {
        String body = "{\"showId\":%d,\"seatIds\":[%s]}".formatted(showId,
                seatIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        String response = mockMvc.perform(authed(post("/api/v1/holds"), token).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf(((Number) JsonPath.read(response, "$.holdId")).longValue());
    }

    protected long createBooking(String token, long holdId, String discountCode) throws Exception {
        String code = discountCode == null ? "" : ",\"discountCode\":\"" + discountCode + "\"";
        String response = mockMvc.perform(authed(post("/api/v1/bookings"), token)
                        .content("{\"holdId\":%d%s}".formatted(holdId, code)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected long createId(String token, String path, String body) throws Exception {
        String response = mockMvc.perform(authed(post(path), token).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    protected String adminToken() throws Exception {
        return login("admin@moviebooking.com", "Admin@12345");
    }

    protected String registerCustomer() throws Exception {
        String email = "bk-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"C\",\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(email)))
                .andExpect(status().isCreated());
        return login(email, "Secret123");
    }

    protected String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
