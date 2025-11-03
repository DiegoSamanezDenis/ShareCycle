package com.sharecycle.service.payment;

import com.sharecycle.domain.model.User;
import com.stripe.exception.StripeException;
import org.springframework.stereotype.Service;

@Service
public interface PaymentGateway {
    boolean capture(double amount, String riderToken) throws PaymentException;
    String createPaymentToken(User user) throws PaymentException;
}
