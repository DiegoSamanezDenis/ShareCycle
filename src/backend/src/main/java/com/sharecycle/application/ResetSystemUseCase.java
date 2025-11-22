package com.sharecycle.application;

import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.service.SeedDataLoader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ResetSystemUseCase {

    private final UserRepository userRepository;
    private final SeedDataLoader seedDataLoader;

    @PersistenceContext
    private EntityManager entityManager;

    public ResetSystemUseCase(UserRepository userRepository,
                              SeedDataLoader seedDataLoader) {
        this.userRepository = userRepository;
        this.seedDataLoader = seedDataLoader;
    }

    @Transactional
    public ResetSummary execute(UUID operatorId) {
        Objects.requireNonNull(operatorId, "operatorId must not be null");
        requireOperator(operatorId);
        clearMutableTables();
        SeedDataLoader.SeedResult result = seedDataLoader.reloadFromSeedFiles();
        return new ResetSummary(result.bikes(), result.stations(), result.docks());
    }

    private void requireOperator(UUID operatorId) {
        User user = userRepository.findById(operatorId);
        if (user == null || user.getRole() == null) {
            throw new SecurityException("Only operators can reset the system.");
        }
        String role = user.getRole().toUpperCase();
        if (!"OPERATOR".equals(role) && !"ADMIN".equals(role)) {
            throw new SecurityException("Only operators can reset the system.");
        }
    }

    private void clearMutableTables() {
        entityManager.createQuery("delete from JpaLedgerEntryEntity").executeUpdate();
        entityManager.createQuery("delete from JpaReservationEntity").executeUpdate();
        entityManager.createQuery("delete from JpaTripEntity").executeUpdate();
        entityManager.createQuery("delete from JpaDockEntity").executeUpdate();
        entityManager.createQuery("delete from JpaBikeEntity").executeUpdate();
        entityManager.createQuery("delete from JpaStationEntity").executeUpdate();
    }

    public record ResetSummary(int bikes, int stations, int docks) {}
}

