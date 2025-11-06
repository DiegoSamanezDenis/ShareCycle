package com.sharecycle.service.payment;

import com.sharecycle.domain.model.User;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class StripePaymentGateway implements PaymentGateway {

    private Logger logger = LoggerFactory.getLogger(StripePaymentGateway.class);
    private final StripePaymentProcessor processor;

    public StripePaymentGateway(StripePaymentProcessor processor) {
        this.processor = processor;
    }

    @Override
    public boolean capture(double amount, String riderToken) throws PaymentException {
        // Stripe uses smallest currency unit (i.e cent for CAD)
        double stripeAmount = amount * 100;

        try {
            PaymentIntent charge = processor.charge((long) stripeAmount, riderToken);
            logger.info("Charge sent to Stripe successfully");
            String status = charge.getStatus();
            logger.info("Charge status: " + status);
            return status.equals("succeeded");
        } catch (StripeException e) {
            throw new PaymentException(e.toString());
        }
    }

    // TODO: create a use case to create actual card token instead of sample card
    @Override
    public String createPaymentToken(User user) throws PaymentException {
        logger.info("Creating Stripe payment token");
        StripePaymentProcessor processor = new StripePaymentProcessor();
        return processor.createSampleCardToken();
    }


}
