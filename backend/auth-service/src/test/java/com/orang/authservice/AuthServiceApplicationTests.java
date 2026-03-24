package com.orang.authservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires database - we'll enable this later with Testcontainers")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
