package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.LedgerEntry;
import com.sharecycle.model.entity.Trip;
import com.sharecycle.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface JpaLedgerEntryRepository {
    void save(LedgerEntry ledgerEntry);
    LedgerEntry findById(UUID id);
    LedgerEntry findByTrip(Trip trip);
    List<LedgerEntry> findAllByUser(User user);

}
