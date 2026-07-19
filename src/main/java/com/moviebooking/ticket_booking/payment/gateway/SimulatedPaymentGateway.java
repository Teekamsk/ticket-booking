package com.moviebooking.ticket_booking.payment.gateway;

import com.moviebooking.ticket_booking.payment.config.PaymentProperties;
import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/** Simulates a gateway: waits the configured delay, then succeeds unless a random draw hits failureRate. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedPaymentGateway implements PaymentGateway {

    private final PaymentProperties properties;
    private final SecureRandom random = new SecureRandom();

    @Override
    public GatewayOutcome charge(long amount, PaymentMethod method) {
        sleep(properties.delay());
        boolean success = random.nextDouble() >= properties.failureRate();
        String txnRef = "SIMTXN-" + HexFormat.of().toHexDigits(random.nextInt()).toUpperCase();
        log.info("Simulated {} charge of {} paise -> {}", method, amount, success ? "SUCCESS" : "FAILED");
        return new GatewayOutcome(success, txnRef);
    }

    private void sleep(Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
