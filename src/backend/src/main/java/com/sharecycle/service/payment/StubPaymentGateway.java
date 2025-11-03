package com.sharecycle.service.payment;

import com.sharecycle.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
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
