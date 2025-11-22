package com.sharecycle.application;

import com.sharecycle.service.payment.PaymentException;
import com.sharecycle.service.payment.PaymentGateway;
import com.sharecycle.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@ContextConfiguration(classes = {StationControllerTest.TestPaymentGatewayConfig.class})
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listStationsReturns200() throws Exception {
        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk());
    }

    /**
     * Test configuration providing a stub PaymentGateway bean.
     * Marked @Primary to resolve multiple PaymentGateway beans conflict.
     */
    @TestConfiguration
    static class TestPaymentGatewayConfig {

        @Bean
        @Primary
        public PaymentGateway paymentGateway() {
            return new PaymentGateway() {
                @Override
                public boolean capture(double amount, String riderToken) throws PaymentException {
                    return true; // always succeed
                }

                @Override
                public String createPaymentToken(User user) throws PaymentException {
                    return "dummy-token"; // dummy token for tests
                }
            };
        }
    }
}
