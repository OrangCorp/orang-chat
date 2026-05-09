package com.orang.userservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Integration test; requires local DB on localhost:5433")
@SpringBootTest
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
