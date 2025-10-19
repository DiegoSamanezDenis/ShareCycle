package com.sharecycle.model.entity;


// Not stored via database, simply generated through Trip in LedgerEntry
public class Bill {

    private final double taxRate = 0.14975;
    private double subtotal;
    private double taxAmount;
    private double total;

    public Bill(Trip trip) {
        calculateSubtotal(trip);
        calculateTax();
        calculateTotal();
    }

    private void calculateSubtotal(Trip trip) {
        subtotal = trip.getDurationMinutes()*0.05; //I'm not businessman, I don't know what's good price
    }
    private void calculateTax() {
        taxAmount = subtotal*taxRate;
    }
    private void calculateTotal() {
        total = subtotal + taxAmount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
