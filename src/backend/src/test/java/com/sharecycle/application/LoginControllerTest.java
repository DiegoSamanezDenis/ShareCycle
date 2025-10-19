package com.sharecycle.application;

import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        String hashed = BCrypt.hashpw("password123", BCrypt.gensalt());
        Rider rider = new Rider("Login User", "Addr", "login@example.com", "loginuser", hashed, "tok");
        rider.setRole("RIDER");
        userRepository.save(rider);
    }

    @Test
    void loginReturns200() throws Exception {
        String body = "{\n" +
                "  \"username\": \"loginuser\",\n" +
                "  \"password\": \"password123\"\n" +
                "}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}


