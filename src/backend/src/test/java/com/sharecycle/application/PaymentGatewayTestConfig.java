package com.sharecycle.application;

import com.sharecycle.domain.model.User;
import com.sharecycle.service.payment.PaymentException;
import com.sharecycle.service.payment.PaymentGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Configuration class for providing a stubbed PaymentGateway implementation
 * exclusively for integration and unit tests.
 */
@TestConfiguration
public class PaymentGatewayTestConfig {

    /**
     * Defines a stub PaymentGateway bean that simulates payment success
     * without relying on an external payment provider.
     *
     * @return A stub implementation of PaymentGateway.
     */
    @Bean
    public PaymentGateway paymentGateway() {
        return new PaymentGateway() {
            /**
             * Always succeeds for testing purposes.
             *
             * @param amount The amount to capture (ignored).
             * @param riderToken The token representing the rider's payment method (ignored).
             * @return true, indicating successful capture.
             * @throws PaymentException Should not be thrown in this stub.
             */
            @Override
            public boolean capture(double amount, String riderToken) throws PaymentException {
                // Simulation: Assume capture always succeeds in test environment
                return true;
            }

            /**
             * Returns a hardcoded dummy token for testing purposes.
             *
             * @param user The user for whom the token is created (ignored).
             * @return A fixed dummy payment token string.
             * @throws PaymentException Should not be thrown in this stub.
             */
            @Override
            public String createPaymentToken(User user) throws PaymentException {
                // Simulation: Return a known, dummy token for testing
                return "dummy-token";
            }
        };
    }
}