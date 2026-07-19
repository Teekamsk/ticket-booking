package com.moviebooking.ticket_booking.payment.gateway;

import com.moviebooking.ticket_booking.payment.config.PaymentProperties;
import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    @Test
    void zeroFailureRate_alwaysSucceeds() {
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(new PaymentProperties(Duration.ZERO, 0.0));
        GatewayOutcome outcome = gateway.charge(20000, PaymentMethod.UPI);
        assertThat(outcome.success()).isTrue();
        assertThat(outcome.txnRef()).startsWith("SIMTXN-");
    }

    @Test
    void fullFailureRate_alwaysFails() {
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(new PaymentProperties(Duration.ZERO, 1.0));
        assertThat(gateway.charge(20000, PaymentMethod.CARD).success()).isFalse();
    }
}
