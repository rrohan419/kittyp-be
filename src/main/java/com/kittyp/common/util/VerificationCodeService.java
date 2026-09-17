package com.kittyp.common.util;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kittyp.common.exception.CustomException;

@Service
public class VerificationCodeService {

    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int MAX_SENDS_PER_WINDOW = 5;

    private final Cache<String, String> codeCache;
    private final Cache<String, Boolean> verifiedCache;
    private final Cache<String, AtomicInteger> attemptCache;
    private final Cache<String, AtomicInteger> sendRateCache;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationCodeService() {
        this.codeCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        this.verifiedCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        this.attemptCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        this.sendRateCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public String generateCode(String key) {
        enforceSendRateLimit(key);
        String existing = codeCache.getIfPresent(key);
        if (existing != null && !existing.isBlank()) {
            attemptCache.invalidate(key);
            return existing;
        }
        int code = 100_000 + secureRandom.nextInt(900_000);
        String codeStr = String.valueOf(code);
        codeCache.put(key, codeStr);
        attemptCache.invalidate(key);
        return codeStr;
    }

    public boolean verifyCode(String key, String code, boolean deactivateCode) {
        if (key == null || code == null || code.isBlank()) {
            return false;
        }

        AtomicInteger attempts = attemptCache.get(key, k -> new AtomicInteger(0));
        if (attempts != null && attempts.get() >= MAX_VERIFY_ATTEMPTS) {
            throw new CustomException("Too many invalid OTP attempts. Please request a new code.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String cachedCode = codeCache.getIfPresent(key);
        if (cachedCode != null && cachedCode.equals(code.trim())) {
            if (deactivateCode) {
                codeCache.invalidate(key);
            }
            attemptCache.invalidate(key);
            return true;
        }

        if (attempts != null) {
            attempts.incrementAndGet();
        }
        return false;
    }

    /** Peek whether code matches without consuming attempts or deactivating. */
    public boolean codeMatches(String key, String code) {
        if (key == null || code == null || code.isBlank()) {
            return false;
        }
        String cached = codeCache.getIfPresent(key);
        return cached != null && cached.equals(code.trim());
    }

    public void markVerified(String key) {
        verifiedCache.put(key, Boolean.TRUE);
    }

    public boolean isVerified(String key) {
        return Boolean.TRUE.equals(verifiedCache.getIfPresent(key));
    }

    public void clearVerified(String key) {
        verifiedCache.invalidate(key);
    }

    public void clearAttempts(String key) {
        if (key == null) {
            return;
        }
        attemptCache.invalidate(key);
        codeCache.invalidate(key);
    }

    private void enforceSendRateLimit(String key) {
        AtomicInteger sends = sendRateCache.get(key, k -> new AtomicInteger(0));
        if (sends != null && sends.incrementAndGet() > MAX_SENDS_PER_WINDOW) {
            throw new CustomException("Too many OTP requests. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    /** Last-10 digits with +91 prefix so send/verify share one phone OTP cache key. */
    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() >= 10) {
            String local10 = digits.substring(digits.length() - 10);
            return "+91" + local10;
        }
        return phone.trim();
    }

    public static String emailOtpKey(String email) {
        return "signup-email:" + email.trim().toLowerCase();
    }

    public static String phoneOtpKey(String phone) {
        return "signup-phone:" + normalizePhone(phone);
    }

    public static String emailVerifiedKey(String email) {
        return "signup-email-ok:" + email.trim().toLowerCase();
    }

    public static String phoneVerifiedKey(String phone) {
        return "signup-phone-ok:" + normalizePhone(phone);
    }

    public static String profileEmailOtpKey(String userUuid, String email) {
        return "profile-email:" + userUuid + ":" + email.trim().toLowerCase();
    }

    public static String profilePhoneOtpKey(String userUuid, String phone) {
        return "profile-phone:" + userUuid + ":" + phone.trim();
    }

    public static String profileEmailVerifiedKey(String userUuid, String email) {
        return "profile-email-ok:" + userUuid + ":" + email.trim().toLowerCase();
    }

    public static String profilePhoneVerifiedKey(String userUuid, String phone) {
        return "profile-phone-ok:" + userUuid + ":" + phone.trim();
    }

    /** One-pet consent OTP: staff adding a pet on behalf of an existing owner (email channel). */
    public static String clinicPetConsentOtpKey(String clinicUuid, String ownerEmail, String petName) {
        return "clinic-pet-consent:" + clinicUuid.trim() + ":" + normalizeEmail(ownerEmail) + ":"
                + normalizePetName(petName);
    }

    public static String clinicPetConsentVerifiedKey(String clinicUuid, String ownerEmail, String petName) {
        return "clinic-pet-consent-ok:" + clinicUuid.trim() + ":" + normalizeEmail(ownerEmail) + ":"
                + normalizePetName(petName);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String normalizePetName(String petName) {
        return petName == null ? "" : petName.trim().toLowerCase();
    }
}
