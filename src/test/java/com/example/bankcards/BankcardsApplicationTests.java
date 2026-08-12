package com.example.bankcards;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the full application context (JPA, Liquibase, Security, JWT,
 * Swagger) must wire up successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class BankcardsApplicationTests {

    @Test
    void contextLoads() {
    }
}
