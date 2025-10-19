package com.sharecycle.domain.model;

public class Bill {

    private static final double TAX_RATE = 0.14975;

    private double subtotal;
    private double taxAmount;
    private double total;

    public Bill(Trip trip) {
        calculateSubtotal(trip);
        calculateTax();
        calculateTotal();
    }

    private void calculateSubtotal(Trip trip) {
        this.subtotal = trip.getDurationMinutes() * 0.05;
    }

    private void calculateTax() {
        this.taxAmount = subtotal * TAX_RATE;
    }

    private void calculateTotal() {
        this.total = subtotal + taxAmount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getTotal() {
        return total;
    }
}
