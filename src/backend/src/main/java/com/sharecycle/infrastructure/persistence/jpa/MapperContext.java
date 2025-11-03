package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapperContext {
    final Map<UUID, Station> stations = new HashMap<>();
    final Map<UUID, Bike> bikes = new HashMap<>();
    final Map<UUID, Dock> docks = new HashMap<>();
    final Map<UUID, Rider> riders = new HashMap<>();
    final Map<UUID, User> users = new HashMap<>();
    final Map<UUID, Trip> trips = new HashMap<>();
    final Map<UUID, Reservation> reservations = new HashMap<>();
    final Map<UUID, LedgerEntry> ledgers = new HashMap<>();

    final Map<UUID, JpaStationEntity> stationEntities = new HashMap<>();
    final Map<UUID, JpaBikeEntity> bikeEntities = new HashMap<>();
    final Map<UUID, JpaReservationEntity> reservationEntities = new HashMap<>();
    final Map<UUID, JpaTripEntity> tripEntities = new HashMap<>();
    final Map<UUID, JpaLedgerEntryEntity> ledgerEntities = new HashMap<>();
    final Map<UUID, JpaUserEntity> userEntities = new HashMap<>();
    public MapperContext() {
    }
}
