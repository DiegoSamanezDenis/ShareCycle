package com.sharecycle.application;

import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String username;

    @BeforeEach
    void setUp() {
        username = "loginuser-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = username + "@example.com";
        String hashed = BCrypt.hashpw("password123", BCrypt.gensalt());
        Rider rider = new Rider("Login User", "Addr", email, username, hashed, "tok", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(rider);
    }

    @Test
    void loginReturns200() throws Exception {
        String body = "{\n" +
                "  \"username\": \"" + username + "\",\n" +
                "  \"password\": \"password123\"\n" +
                "}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
