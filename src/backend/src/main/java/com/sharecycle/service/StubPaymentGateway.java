package com.sharecycle.service;

import com.sharecycle.domain.model.User;
import com.stripe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubPaymentGateway  implements PaymentGateway {
    Logger logger = LoggerFactory.getLogger(StubPaymentGateway.class);
    @Override
    public boolean capture(double amount, String riderToken) {
        logger.info("Capture stub payment gateway started");
        logger.info("Amount: {}", amount);
        logger.info("Rider Token: {}", riderToken);
        logger.info("Capture stub payment gateway successful");
        return true;
    }

    @Override
    public String createPaymentToken(User user) {
        return "";
    }
}
