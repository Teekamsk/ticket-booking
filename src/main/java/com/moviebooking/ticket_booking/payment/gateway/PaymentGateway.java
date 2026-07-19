package com.moviebooking.ticket_booking.payment.gateway;

import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;

/** Abstraction over a payment provider. Only a simulated implementation exists in this project. */
public interface PaymentGateway {

    GatewayOutcome charge(long amount, PaymentMethod method);
}
