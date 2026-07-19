package com.moviebooking.ticket_booking.booking;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The headline correctness property: concurrent holds on the same seat must serialize — exactly one
 * succeeds, the rest get 409. Runs against real Postgres so SELECT … FOR UPDATE is genuinely exercised.
 */
class BookingConcurrencyTest extends AbstractBookingIntegrationTest {

    @Test
    void concurrentHoldsOnSameSeat_exactlyOneSucceeds() throws Exception {
        int threads = 8;
        ShowFixture show = createShow(1);
        Long seatId = show.seatIds().get(0);

        // Pre-mint one customer token per thread (registration is not part of the race).
        String[] tokens = new String[threads];
        for (int i = 0; i < threads; i++) {
            tokens[i] = registerCustomer();
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        try {
            List<Future<Integer>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String token = tokens[i];
                Callable<Integer> task = () -> {
                    startGate.await();
                    int statusCode = mockMvc.perform(authed(post("/api/v1/holds"), token)
                                    .content("{\"showId\":%d,\"seatIds\":[%d]}".formatted(show.showId(), seatId)))
                            .andReturn().getResponse().getStatus();
                    if (statusCode == 201) {
                        created.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflicts.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                    return statusCode;
                };
                futures.add(pool.submit(task));
            }
            startGate.countDown();
            for (Future<Integer> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(created.get()).as("exactly one hold succeeds").isEqualTo(1);
        assertThat(conflicts.get()).as("all others get 409").isEqualTo(threads - 1);
        assertThat(other.get()).as("no unexpected statuses").isZero();

        // The seat is HELD exactly once.
        mockMvc.perform(get("/api/v1/shows/" + show.showId() + "/seats"))
                .andExpect(jsonPath("$.seats[0].status", is("HELD")));
    }
}
