package com.sharecycle.application;

import com.sharecycle.ui.RegistrationController;
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
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerReturns201() throws Exception {
        String body = "{\n" +
                "  \"fullName\": \"Test User\",\n" +
                "  \"streetAddress\": \"123 St\",\n" +
                "  \"email\": \"test@example.com\",\n" +
                "  \"username\": \"testuser\",\n" +
                "  \"password\": \"password123\",\n" +
                "  \"paymentMethodToken\": \"tok_123\",\n" +
                "  \"pricingPlanType\": \"PAY_AS_YOU_GO\"\n" +
                "}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}


