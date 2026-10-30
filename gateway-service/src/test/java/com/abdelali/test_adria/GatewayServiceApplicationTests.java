package com.abdelali.test_adria;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "server.port=0",
        "eureka.client.enabled=false"
})
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies MVC Gateway routes bind and Spring Cloud services initialize.
    }
}
