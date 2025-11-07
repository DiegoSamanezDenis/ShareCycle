package com.sharecycle.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBillRepository;

@SpringBootTest
@ActiveProfiles("test")
class JpaBillRepositoryImplTest {

    @Autowired
    private JpaBillRepository billRepository;

    @Test
    void testBillRepositoryIsInjected() {
        assertNotNull(billRepository, "JpaBillRepository should be injected");
    }

    @Test
    void testFindByIdReturnsNullWhenNotFound() {
        UUID randomId = UUID.randomUUID();
        Bill bill = billRepository.findById(randomId);
        assertNull(bill, "Should return null for non-existent bill ID");
    }

    @Test
    void testFindAllByUserReturnsEmptyListForUserWithNoBills() {
        User testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        
        List<Bill> bills = billRepository.findAllByUser(testUser);
        
        assertNotNull(bills, "Should return non-null list");
        assertTrue(bills.isEmpty(), "Should return empty list for user with no bills");
    }

    @Test
    void testFindByDateRangeReturnsEmptyListForFutureRange() {
        LocalDateTime futureStart = LocalDateTime.now().plusYears(10);
        LocalDateTime futureEnd = LocalDateTime.now().plusYears(11);
        
        List<Bill> bills = billRepository.findByDateRange(futureStart, futureEnd);
        
        assertNotNull(bills, "Should return non-null list");
        assertTrue(bills.isEmpty(), "Should return empty list for future date range");
    }
}
