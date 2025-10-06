package com.sharecycle;

import com.sharecycle.model.entity.Operator;
import com.sharecycle.model.entity.Rider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDateTime;

public class SharecycleApplication {

  public static void main(String[] args) {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
    EntityManager em = emf.createEntityManager();

    em.getTransaction().begin();

    Rider rider = new Rider(
            "Bhaskar",
            "Concordia",
            "bhaskar@example.com",
            "bd",
            "hashedPassword",
            null
    );

    Operator operator = new Operator(
            "BhaskarDas",
            "Concordia",
            "bhaskard@example.com",
            "bdop",
            "hashedPassword",
            null
    );

    em.persist(rider);   // Hibernate assigns ID automatically
    em.persist(operator);

    em.getTransaction().commit();

    em.close();
    emf.close();

    System.out.println("Rider and Operator successfully persisted!");
  }
}
