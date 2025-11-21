package com.sharecycle.backend; // keep whatever package you have

import com.sharecycle.application.PaymentGatewayTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PaymentGatewayTestConfig.class)
class SharecycleBackendApplicationTests {

  @Test
  void contextLoads() { }
}
