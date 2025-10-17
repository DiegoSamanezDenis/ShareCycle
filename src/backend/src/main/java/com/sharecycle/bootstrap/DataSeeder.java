package com.sharecycle.bootstrap;


import com.sharecycle.application.RegisterOperatorUseCase;
import com.sharecycle.infrastructure.JpaUserRepository;
import com.sharecycle.service.BcryptHasher;
import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataSeeder {


    private final JpaUserRepository userRepository;
    private final BcryptHasher bcryptHasher;
private final RegisterOperatorUseCase registerOperatorUseCase;

    public DataSeeder(JpaUserRepository userRepository, BcryptHasher bcryptHasher, RegisterOperatorUseCase registerOperatorUseCase) {
        this.userRepository = userRepository;
        this.bcryptHasher = bcryptHasher;
        this.registerOperatorUseCase = registerOperatorUseCase;
    }


//    public void init()

    //to make it run on startup (bascally after all beans are initialised/setup)
    @PostConstruct
    public void init() {
        if(!userRepository.existsByUsername("SmoothOperator")){
            registerOperatorUseCase.register(
"Smoooothhh operatorrr",
                    "smooth address",
                    "smooth@op.com",
                    "SmoothOperator",
                    bcryptHasher.hash("wowpass"),
                    null

            );
        }
        System.out.println("DataSeeder running...");

    }
}
