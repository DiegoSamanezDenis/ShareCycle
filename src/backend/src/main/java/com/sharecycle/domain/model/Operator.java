package com.sharecycle.domain.model;

import java.util.UUID;

public class Operator extends User {

    private String currentMode; // "OPERATOR" or "RIDER"

    public Operator() {
        super.setRole("OPERATOR");
        this.currentMode = "OPERATOR";
    }

    public Operator(User user) {
        super(user.getUserId(),
                user.getFullName(),
                user.getStreetAddress(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                "OPERATOR",
                user.getPaymentMethodToken(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getFlexCredit());
        this.currentMode = "OPERATOR";
    }

    public Operator(String fullName,
                    String streetAddress,
                    String email,
                    String username,
                    String passwordHash,
                    String paymentMethodToken) {
        super(UUID.randomUUID(),
                fullName,
                streetAddress,
                email,
                username,
                passwordHash,
                "OPERATOR",
                paymentMethodToken,
                null,
                null,
                0);
        this.currentMode = "OPERATOR";
    }

    /**
     * Get the current mode the operator is working in
     * @return "OPERATOR" or "RIDER"
     */
    public String getCurrentMode() {
        return currentMode;
    }

    /**
     * Toggle between OPERATOR and RIDER modes
     * Operators can switch to RIDER mode to test rider functionality
     */
    public void toggleMode() {
        if ("OPERATOR".equals(currentMode)) {
            this.currentMode = "RIDER";
        } else {
            this.currentMode = "OPERATOR";
        }
    }

    /**
     * Set the mode explicitly
     * @param mode "OPERATOR" or "RIDER"
     * @throws IllegalArgumentException if mode is not valid
     */
    public void setMode(String mode) {
        if (!"OPERATOR".equals(mode) && !"RIDER".equals(mode)) {
            throw new IllegalArgumentException("Mode must be either 'OPERATOR' or 'RIDER'");
        }
        this.currentMode = mode;
    }

    /**
     * Check if operator is currently in RIDER mode
     * @return true if in RIDER mode, false if in OPERATOR mode
     */
    public boolean isInRiderMode() {
        return "RIDER".equals(currentMode);
    }

    /**
     * Check if operator is currently in OPERATOR mode
     * @return true if in OPERATOR mode, false if in RIDER mode
     */
    public boolean isInOperatorMode() {
        return "OPERATOR".equals(currentMode);
    }

    /**
     * Get the effective role for the current mode
     * This is used by the API to determine available actions
     * @return current mode ("OPERATOR" or "RIDER")
     */
    public String getEffectiveRole() {
        return currentMode;
    }
}
