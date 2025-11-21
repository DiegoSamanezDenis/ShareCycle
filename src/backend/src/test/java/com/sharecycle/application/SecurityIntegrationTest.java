package com.sharecycle.application;

import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.service.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PaymentGatewayTestConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionStore sessionStore;

    private String token;

    @BeforeEach
    void setUp() {
        String username = "secure-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = username + "@example.com";
        String hashed = BCrypt.hashpw("secret123", BCrypt.gensalt());
        Rider rider = new Rider("Secure Rider", "123 Rider St", email, username, hashed, "tok", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(rider);
        token = sessionStore.createSession(rider.getUserId());
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAllowsValidToken() throws Exception {
        mockMvc.perform(get("/api/stations")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void publicStationsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public/stations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
