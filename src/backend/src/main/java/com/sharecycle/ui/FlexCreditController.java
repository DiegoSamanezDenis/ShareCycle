package com.sharecycle.ui;

import com.sharecycle.domain.model.User;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class FlexCreditController {
    private final JpaUserRepository userRepository;

    public FlexCreditController(JpaUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/credit")
    public Map<String, Double> getCredit(@RequestParam UUID userId) {
        User user = userRepository.findById(userId);
        double credit = user.getFlexCredit();
        System.out.println("Flex credit:" + credit);
        return Map.of("amount", credit);
    }

}
