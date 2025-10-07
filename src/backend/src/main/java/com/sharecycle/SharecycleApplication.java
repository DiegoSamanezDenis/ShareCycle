package com.sharecycle;

import com.sharecycle.application.RegisterRiderUseCase;
import com.sharecycle.infrastructure.JpaUserRepository;
import com.sharecycle.model.entity.Rider;
import com.sharecycle.service.BcryptHasher;
import com.sharecycle.service.BcryptHasher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SharecycleApplication {

  public static void main(String[] args) {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
    EntityManager em = emf.createEntityManager();

    em.getTransaction().begin();

    // Setup repository and password hasher
    JpaUserRepository userRepository = new JpaUserRepository(em);
    BcryptHasher passwordHasher = new BcryptHasher();

    // Create use case
    RegisterRiderUseCase registerRiderUseCase = new RegisterRiderUseCase(userRepository, passwordHasher);

    // Register a rider
    Rider rider = registerRiderUseCase.register(
            "Bhaskar",
            "Concordia",
            "bhaskar4@example.com",
            "bhaskar4",
            "securePassword123",
            "tok_visa_123"
    );

    System.out.println("Registered Rider: " + rider.getFullName() + ", email: " + rider.getEmail());

    em.getTransaction().commit();
    em.close();
    emf.close();
  }
}
