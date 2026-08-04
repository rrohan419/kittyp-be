package com.kittyp.common.util;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class VerificationCodeService {

    private final Cache<String, String> codeCache;
    private final Cache<String, Boolean> verifiedCache;
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
    }

    public String generateCode(String key) {
        int code = 100_000 + secureRandom.nextInt(900_000);
        String codeStr = String.valueOf(code);
        codeCache.put(key, codeStr);
        return codeStr;
    }

    public boolean verifyCode(String key, String code, boolean deactivateCode) {
        String cachedCode = codeCache.getIfPresent(key);
        if (cachedCode != null && cachedCode.equals(code)) {
            if (deactivateCode) {
                codeCache.invalidate(key);
            }
            return true;
        }
        return false;
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

    public static String emailOtpKey(String email) {
        return "signup-email:" + email.trim().toLowerCase();
    }

    public static String phoneOtpKey(String phone) {
        return "signup-phone:" + phone.trim();
    }

    public static String emailVerifiedKey(String email) {
        return "signup-email-ok:" + email.trim().toLowerCase();
    }

    public static String phoneVerifiedKey(String phone) {
        return "signup-phone-ok:" + phone.trim();
    }
}
