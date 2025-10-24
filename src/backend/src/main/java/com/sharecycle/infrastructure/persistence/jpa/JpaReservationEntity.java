package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation")
public class JpaReservationEntity {

    @Id
    @Column(name = "reservation_id", columnDefinition = "BINARY(16)")
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "rider_id")
    private JpaUserEntity.JpaRiderEntity rider;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "station_id")
    private JpaStationEntity station;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "bike_id")
    private JpaBikeEntity bike;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "expires_after_minutes")
    private int expiresAfterMinutes;

    @Column(name = "active")
    private boolean active;

    public JpaReservationEntity() {
    }

    private JpaReservationEntity(Reservation reservation) {
        this.reservationId = reservation.getReservationId();
        this.reservedAt = reservation.getReservedAt();
        this.expiresAt = reservation.getExpiresAt();
        this.expiresAfterMinutes = reservation.getExpiresAfterMinutes();
        this.active = reservation.isMarkedActive();
    }

    public static JpaReservationEntity fromDomain(Reservation reservation, MapperContext context) {
        JpaReservationEntity existing = context.reservationEntities.get(reservation.getReservationId());
        if (existing != null) {
            return existing;
        }
        JpaReservationEntity entity = new JpaReservationEntity(reservation);
        Rider riderDomain = reservation.getRider();
        entity.rider = (JpaUserEntity.JpaRiderEntity) JpaUserEntity.fromDomain(riderDomain);
        entity.station = JpaStationEntity.fromDomain(reservation.getStation(), context);
        entity.bike = JpaBikeEntity.fromDomain(reservation.getBike(), context);
        context.reservationEntities.put(reservation.getReservationId(), entity);
        context.reservations.put(reservation.getReservationId(), reservation);
        return entity;
    }

    public Reservation toDomain(MapperContext context) {
        Reservation existing = context.reservations.get(reservationId);
        if (existing != null) {
            return existing;
        }
        Rider riderDomain = rider.toDomain();
        Reservation reservation = new Reservation(
                reservationId,
                riderDomain,
                station.toDomain(context),
                bike.toDomain(context),
                reservedAt,
                expiresAt,
                expiresAfterMinutes,
                active
        );
        context.reservations.put(reservationId, reservation);
        return reservation;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }
}
