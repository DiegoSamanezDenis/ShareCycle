package com.sharecycle.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.MissingResourceException;

// TODO: Actually implement card token instead of using sample
@Service
@Configuration
@PropertySource("classpath:.env.payment")
public class StripePaymentProcessor {

    Logger logger = LoggerFactory.getLogger(StripePaymentProcessor.class);

    @Value("${STRIPE_SECRET_KEY}")
    private String STRIPE_SECRET_KEY;

    @PostConstruct
    public void init() {
        Stripe.apiKey = STRIPE_SECRET_KEY;
    }

    @PostConstruct
    public void checkKeyLoaded() {
        if ((STRIPE_SECRET_KEY == null)) {
            throw new MissingResourceException("Stripe Secret Key", StripePaymentProcessor.class.getName(), "StripeSecretKey");
        }
        logger.info("Stripe Secret Key is loaded");
    }

    // For now rider token is not used
    public PaymentIntent charge(long amount, String riderToken)
            throws StripeException {
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amount)
                        .setCurrency("cad")
                        .setPaymentMethod("pm_card_ca")
                        .addPaymentMethodType("card")
                        .setConfirm(true)
                        .build();

        return PaymentIntent.create(params);
    }

    public String createSampleCardToken() throws PaymentException {
        return "";
    }
}
