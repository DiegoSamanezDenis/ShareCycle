package com.sharecycle.application;

import com.sharecycle.domain.event.*;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import com.sharecycle.service.payment.PaymentException;
import com.sharecycle.service.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentUseCase {
    private final Logger logger = LoggerFactory.getLogger(PaymentUseCase.class);
    private static final double MIN_STRIPE_AMOUNT_CAD = 0.50d;
    private final JpaLedgerEntryRepository ledgerEntryRepository;
    private final JpaUserRepository userRepository;
    private final DomainEventPublisher eventPublisher;
    private final PaymentGateway paymentGateway;

    @Autowired
    public PaymentUseCase(JpaLedgerEntryRepository ledgerEntryRepository,
                          JpaUserRepository userRepository,
                          DomainEventPublisher domainEventPublisher,
                          PaymentGateway paymentGateway) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.eventPublisher = domainEventPublisher;
        this.paymentGateway = paymentGateway;
    }

    public LedgerEntry execute(LedgerEntry ledgerEntry) {
        if (ledgerEntry == null) {
            logger.warn("No ledger entry supplied for payment");
            return null;
        }
        LedgerEntry managedLedgerEntry = ledgerEntryRepository.findById(ledgerEntry.getLedgerId());
        LedgerEntry entry = managedLedgerEntry != null ? managedLedgerEntry : ledgerEntry;
        Bill bill = entry.getBill();
        double totalAmount = bill != null ? bill.getTotalCost() : 0.0;
        User rider = entry.getUser();
        if (rider == null) {
            logger.warn("Ledger entry {} is missing rider context", entry.getLedgerId());
            return entry;
        }

        if (totalAmount <= 0) {
            markLedgerPaid(entry, rider, "No payment required");
            return entry;
        }

        // Use flex credit before actually paying
        // Get back the overflow amount if too much money
        double flexCreditInitial = rider.getFlexCredit();
        totalAmount = rider.deductFlexCredit(totalAmount);
        double amountDeducted = flexCreditInitial - rider.getFlexCredit();
        userRepository.save(rider);
        eventPublisher.publish(new FlexCreditDeductedEvent(rider.getUserId(), amountDeducted));

        if (totalAmount < MIN_STRIPE_AMOUNT_CAD) {
            logger.info("Ledger {} total {} below Stripe minimum, marking paid without capture",
                    entry.getLedgerId(), totalAmount);
            markLedgerPaid(entry, rider, "Below Stripe minimum; treated as paid");
            return entry;
        }

        String userPaymentToken = this.getOrCreatePaymentToken(rider);

        logger.info("Payment starting for ledger {}", entry.getLedgerId());
        eventPublisher.publish(new PaymentStartedEvent(
                rider.getUserId(),
                entry.getTrip() != null ? entry.getTrip().getTripID() : null,
                "Payment Started"
        ));
        try {
            boolean isSuccess = paymentGateway.capture(totalAmount, userPaymentToken);
            if (isSuccess) {
                markLedgerPaid(entry, rider, "Payment Succeeded");
            } else {
                emitPaymentFailed(entry, rider, "Payment failed");
            }
        } catch (PaymentException tokenException) {
            logger.warn("Payment token {} rejected; generating Stripe test token", userPaymentToken);
            String fallbackToken = paymentGateway.createPaymentToken(rider);
            rider.setPaymentMethodToken(fallbackToken);
            userRepository.save(rider);
            try {
                boolean retrySuccess = paymentGateway.capture(totalAmount, fallbackToken);
                if (retrySuccess) {
                    markLedgerPaid(entry, rider, "Payment Succeeded");
                } else {
                    emitPaymentFailed(entry, rider, "Payment failed");
                }
            } catch (RuntimeException retryEx) {
                logger.error("Payment retry failed for ledger {}", entry.getLedgerId(), retryEx);
                emitPaymentFailed(entry, rider, "Payment failed: " + retryEx.getMessage());
            }
        } catch (RuntimeException ex) {
            logger.error("Payment processing failed for ledger {}", entry.getLedgerId(), ex);
            emitPaymentFailed(entry, rider, "Payment failed: " + ex.getMessage());
        }
        return entry;
    }

    private void markLedgerPaid(LedgerEntry entry, User rider, String message) {
        logger.info("Marking ledger {} as paid", entry.getLedgerId());
        entry.setLedgerStatus(LedgerEntry.LedgerStatus.PAID);
        ledgerEntryRepository.save(entry);
        eventPublisher.publish(new PaymentSucceedEvent(
                rider.getUserId(),
                entry.getTrip() != null ? entry.getTrip().getTripID() : null,
                message
        ));
    }

    private void emitPaymentFailed(LedgerEntry entry, User rider, String message) {
        eventPublisher.publish(new PaymentFailedEvent(
                rider.getUserId(),
                entry.getTrip() != null ? entry.getTrip().getTripID() : null,
                message
        ));
    }

    private String getOrCreatePaymentToken(User user) {
        String existing = user.getPaymentMethodToken();
        if (existing != null && !existing.isBlank() && existing.startsWith("pm_")) {
            return existing;
        }
        logger.info("Generating Stripe test payment token for user {}", user.getUserId());
        String token = paymentGateway.createPaymentToken(user);
        user.setPaymentMethodToken(token);
        userRepository.save(user);
        return token;
    }
}
