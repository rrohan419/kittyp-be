package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.kittyp.common.exception.CustomException;

class LoginRateLimiterTest {

    @Test
    void blocksAfterMaxFailures() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String ip = "1.2.3.4";
        String email = "user@test.com";

        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> limiter.assertAllowed(ip, email));
            limiter.recordFailure(ip, email);
        }
        assertThrows(CustomException.class, () -> limiter.assertAllowed(ip, email));
    }

    @Test
    void successClearsWindow() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String ip = "1.2.3.4";
        String email = "user@test.com";
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip, email);
        }
        limiter.clear(ip, email);
        assertDoesNotThrow(() -> limiter.assertAllowed(ip, email));
    }
}
