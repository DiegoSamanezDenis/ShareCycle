package com.sharecycle.service.payment;

import com.sharecycle.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("test")
public class StubPaymentGateway implements PaymentGateway {
    private static final Logger logger = LoggerFactory.getLogger(StubPaymentGateway.class);

    @Override
    public boolean capture(double amount, String riderToken) {
        logger.info("Stub capture invoked amount={} token={}", amount, riderToken);
        logger.info("Stub payment gateway returning success (demo only)");
        return true;
    }

    @Override
    public String createPaymentToken(User user) {
        String token = "stub_tok_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        logger.info("Generated stub payment token {} for user {}", token, user != null ? user.getUserId() : "unknown");
        return token;
    }
}
