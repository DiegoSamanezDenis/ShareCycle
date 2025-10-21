package com.sharecycle.bootstrap;


import com.sharecycle.application.RegisterOperatorUseCase;
import com.sharecycle.domain.repository.UserRepository;
import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataSeeder {


    private final UserRepository userRepository;
    private final RegisterOperatorUseCase registerOperatorUseCase;

    public DataSeeder(UserRepository userRepository, RegisterOperatorUseCase registerOperatorUseCase) {
        this.userRepository = userRepository;
        this.registerOperatorUseCase = registerOperatorUseCase;
    }


//    public void init()

    //to make it run on startup (bascally after all beans are initialised/setup)
    @PostConstruct
    public void init() {
        if(!userRepository.existsByUsername("SmoothOperator2")) {
            registerOperatorUseCase.register(
                    "Smoooothhh operatorrr",
                    "smooth address",
                    "smooth2@op.com",
                    "SmoothOperator2",
                    "wowpass",
                    null
            );
        }
    }

}
