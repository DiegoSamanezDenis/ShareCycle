package com.sharecycle.service;

import com.sharecycle.domain.model.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.InvoicePayment;
import com.stripe.model.Token;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;
import java.util.Map;

public interface PaymentGateway {
    boolean capture(double amount, String riderToken);
    String createPaymentToken(User user);
}
