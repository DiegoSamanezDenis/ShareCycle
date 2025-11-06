package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.PaymentFailedEvent;
import com.sharecycle.domain.event.PaymentStartedEvent;
import com.sharecycle.domain.event.PaymentSucceedEvent;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import com.sharecycle.service.payment.PaymentGateway;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentUseCase {
    private final Logger logger = LoggerFactory.getLogger(PaymentUseCase.class);
    private final JpaLedgerEntryRepository ledgerEntryRepository;
    private final JpaUserRepository userRepository;
    private final DomainEventPublisher eventPublisher;
    private final PaymentGateway paymentGateway;

    @Autowired
    public PaymentUseCase(JpaLedgerEntryRepository ledgerEntryRepository, JpaUserRepository userRepository, DomainEventPublisher domainEventPublisher, PaymentGateway paymentGateway) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.eventPublisher = domainEventPublisher;
        this.paymentGateway = paymentGateway;
    }

    public void execute(LedgerEntry ledgerEntry) throws StripeException {
        Bill bill = ledgerEntry.getBill();
        double totalAmount = bill != null ? bill.getTotalCost() : 0.0;
        User rider = ledgerEntry.getUser();
        // Get or create payment token if user doesn't have 1 yet
        String userPaymentToken = this.getOrCreatePaymentToken(rider);

        logger.info("Payment starting");
        eventPublisher.publish(new PaymentStartedEvent(rider.getUserId(), ledgerEntry.getTrip().getTripID(), "Payment Started"));
        boolean isSuccess = paymentGateway.capture(totalAmount, userPaymentToken);

        if (isSuccess) {
            logger.info("Payment successful");
            eventPublisher.publish(new PaymentSucceedEvent(rider.getUserId(), ledgerEntry.getTrip().getTripID(), "Payment Succeed"));
            ledgerEntry.setLedgerStatus(LedgerEntry.LedgerStatus.PAID);
            ledgerEntryRepository.save(ledgerEntry);
        } else {
            logger.info("Payment failed");
            eventPublisher.publish(new PaymentFailedEvent(rider.getUserId(), ledgerEntry.getTrip().getTripID(), "Payment failed"));
        }
    }

    private boolean validate(LedgerEntry ledgerEntry) {
        LedgerEntry managedLedgerEntry = ledgerEntryRepository.findById(ledgerEntry.getLedgerId());
        if (managedLedgerEntry == null) {
            logger.error("LedgerEntry does not exist");
            return false;
        }
        if (ledgerEntry.getLedgerStatus() != LedgerEntry.LedgerStatus.PAID) {
            logger.error("Invalid ledger status: {}", ledgerEntry.getLedgerStatus());
            return false;
        }

        return true;
    }

    private String getOrCreatePaymentToken(User user) {
        // Token existed
        if (user.getPaymentMethodToken() != null) {
            return user.getPaymentMethodToken();
        }
        // Token does not exist, create one
        logger.info("No payment token found, creating new payment token");
        String token = paymentGateway.createPaymentToken(user);
        user.setPaymentMethodToken(token);
        logger.info("Payment token created");
        logger.info("Payment token: {}", token);
        userRepository.save(user);
        return token;
    }


}
