package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;

import java.util.List;
import java.util.UUID;

public interface JpaLedgerEntryRepository {
    void save(LedgerEntry ledgerEntry);
    LedgerEntry findById(UUID id);
    LedgerEntry findByTrip(Trip trip);
    List<LedgerEntry> findAllByUser(User user);
    List<LedgerEntry> findAllByTripIds(List<UUID> tripIds);

}
