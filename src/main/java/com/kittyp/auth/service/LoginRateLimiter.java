package com.kittyp.auth.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kittyp.common.exception.CustomException;

/**
 * Login brute-force protection keyed by IP + email (mirrors OTP send/verify limits).
 */
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;

    private final Cache<String, AtomicInteger> attemptCache;

    public LoginRateLimiter() {
        this.attemptCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(50_000)
                .build();
    }

    public void assertAllowed(String clientIp, String email) {
        String key = rateKey(clientIp, email);
        AtomicInteger attempts = attemptCache.get(key, k -> new AtomicInteger(0));
        if (attempts != null && attempts.get() >= MAX_ATTEMPTS_PER_WINDOW) {
            throw new CustomException("Too many login attempts. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void recordFailure(String clientIp, String email) {
        String key = rateKey(clientIp, email);
        AtomicInteger attempts = attemptCache.get(key, k -> new AtomicInteger(0));
        if (attempts != null) {
            attempts.incrementAndGet();
        }
    }

    public void clear(String clientIp, String email) {
        attemptCache.invalidate(rateKey(clientIp, email));
    }

    private static String rateKey(String clientIp, String email) {
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        String em = email == null ? "" : email.trim().toLowerCase();
        return "login:" + ip + ":" + em;
    }
}
