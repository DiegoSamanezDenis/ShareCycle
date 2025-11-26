package com.sharecycle.domain.model;

/**
 * Helper methods for recurring bill calculations that are needed outside the Bill entity.
 */
public final class BillUtils {

    private BillUtils() {
    }

    /**
     * Returns the sum of the ride-related components (base, time, e-bike surcharge)
     * before any flex credit is applied. Returns 0 when the bill is null.
     */
    public static double discountedSubtotal(Bill bill) {
        if (bill == null) {
            return 0.0;
        }
        return bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge();
    }

    /**
     * Calculates the monetary value of the loyalty discount using the provided
     * discount rate. The amount is computed from the discounted subtotal so that
     * flex credit does not interfere.
     */
    public static double loyaltyDiscountAmount(Bill bill, double discountRate) {
        if (bill == null || discountRate <= 0.0) {
            return 0.0;
        }
        double subtotalAfterDiscount = discountedSubtotal(bill);
        if (subtotalAfterDiscount <= 0.0) {
            return 0.0;
        }
        double loyaltyMultiplier = 1.0 - discountRate;
        if (loyaltyMultiplier <= 0.0) {
            return subtotalAfterDiscount;
        }
        double subtotalBeforeDiscount = subtotalAfterDiscount / loyaltyMultiplier;
        return Math.max(0.0, subtotalBeforeDiscount - subtotalAfterDiscount);
    }
}
