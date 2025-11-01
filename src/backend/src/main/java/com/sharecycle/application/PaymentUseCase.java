package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.PaymentFailedEvent;
import com.sharecycle.domain.event.PaymentStartedEvent;
import com.sharecycle.domain.event.PaymentSucceedEvent;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import com.sharecycle.service.PaymentGateway;
import com.sharecycle.service.StubPaymentGateway;
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

    @Autowired
    public PaymentUseCase(JpaLedgerEntryRepository ledgerEntryRepository, JpaUserRepository userRepository, DomainEventPublisher domainEventPublisher) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.eventPublisher = domainEventPublisher;
    }

    public void execute(LedgerEntry ledgerEntry) {
        double totalAmount = ledgerEntry.getTotalAmount();
        User rider = ledgerEntry.getUser();
        // Get or create payment token if user doesn't have 1 yet
        String userPaymentToken = this.getOrCreatePaymentToken(rider);

        PaymentGateway paymentGateway = new StubPaymentGateway();
        logger.info("Payment starting");
        eventPublisher.publish(new PaymentStartedEvent(rider.getUserId(), ledgerEntry.getTrip().getTripID()));
        boolean isSuccess = paymentGateway.capture(totalAmount, userPaymentToken);

        if (isSuccess) {
            logger.info("Payment successful");
            eventPublisher.publish(new PaymentSucceedEvent(rider.getUserId(), ledgerEntry.getTrip().getTripID()));
            ledgerEntry.setLedgerStatus(LedgerEntry.LedgerStatus.PAID);
            ledgerEntryRepository.save(ledgerEntry);
        } else {
            logger.info("Payment failed");
            eventPublisher.publish(new PaymentFailedEvent(rider.getUserId(), ledgerEntry.getTrip().getTripID()));
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
        PaymentGateway paymentGateway = new StubPaymentGateway(); //TODO: Switch to Stripe when implemented
        String token = paymentGateway.createPaymentToken(user);
        user.setPaymentMethodToken(token);
        userRepository.save(user);
        return token;
    }


}
