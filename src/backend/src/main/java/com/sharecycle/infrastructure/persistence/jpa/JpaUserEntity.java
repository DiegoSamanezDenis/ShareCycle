package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.Operator;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public class JpaUserEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "payment_method_token")
    private String paymentMethodToken;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "pricing_plan_type")
    private String pricingPlanType;

    @Column(name = "flex_credit")
    private double flexCredit;

    public JpaUserEntity() {
    }

    protected JpaUserEntity(User user) {
        this.userId = user.getUserId();
        this.fullName = user.getFullName();
        this.streetAddress = user.getStreetAddress();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.passwordHash = user.getPasswordHash();
        this.paymentMethodToken = user.getPaymentMethodToken();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.pricingPlanType = user.getPricingPlanType() != null ? user.getPricingPlanType().name() : null;
    }

    public static JpaUserEntity fromDomain(User user) {
        if (user instanceof Rider rider) {
            return new JpaRiderEntity(rider);
        }
        if (user instanceof Operator operator) {
            return new JpaOperatorEntity(operator);
        }
        return new JpaUserEntity(user);
    }

    public User toDomain() {
        User user = new User(userId, fullName, streetAddress, email, username, passwordHash, role, paymentMethodToken, createdAt, updatedAt, flexCredit);
        if (pricingPlanType != null && !pricingPlanType.isBlank()) {
            try {
                user.setPricingPlanType(PricingPlan.PlanType.valueOf(pricingPlanType));
            } catch (IllegalArgumentException ignored) {
                user.setPricingPlanType(null);
            }
        }
        return user;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPaymentMethodToken() {
        return paymentMethodToken;
    }

    public void setPaymentMethodToken(String paymentMethodToken) {
        this.paymentMethodToken = paymentMethodToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPricingPlanType() {
        return pricingPlanType;
    }

    public void setPricingPlanType(String pricingPlanType) {
        this.pricingPlanType = pricingPlanType;
    }

    public double getFlexCredit() {
        return flexCredit;
    }

    public void setFlexCredit(double flexCredit) {
        this.flexCredit = flexCredit;
    }

    @Entity
    @DiscriminatorValue("RIDER")
    public static class JpaRiderEntity extends JpaUserEntity {
        public JpaRiderEntity() {
            super();
        }

        public JpaRiderEntity(Rider rider) {
            super(rider);
        }

        @Override
        public Rider toDomain() {
            User base = super.toDomain();
            return new Rider(base);
        }
    }

    @Entity
    @DiscriminatorValue("OPERATOR")
    public static class JpaOperatorEntity extends JpaUserEntity {
        public JpaOperatorEntity() {
            super();
        }

        public JpaOperatorEntity(Operator operator) {
            super(operator);
        }

        @Override
        public Operator toDomain() {
            User base = super.toDomain();
            return new Operator(base);
        }
    }

    public User toDomain(MapperContext context) {
        User existing = context.users.get(userId);
        if (existing != null) {
            return existing;
        }
        User user;
        if (role.equals("RIDER")) {
            user = new Rider();
        } else {
            user = new Operator();
        }
        user.setFullName(fullName);
        user.setStreetAddress(streetAddress);
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setPasswordHash(passwordHash);
        user.setPaymentMethodToken(paymentMethodToken);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        context.users.put(userId, user);
        if (role.equals("RIDER")) {
            assert user instanceof Rider;
            context.riders.put(userId, (Rider) user);
        }
        return user;
    }
}
