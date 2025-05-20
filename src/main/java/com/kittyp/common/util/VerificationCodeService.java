/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class VerificationCodeService {

    private final Cache<String, String> codeCache;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationCodeService() {
        this.codeCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    /**
     * Generates a 6-digit verification code for the given user ID and stores it in the cache.
     *
     * @param userId The unique identifier for the user.
     * @return The generated 6-digit code.
     */
    public String generateCode(String userUuid) {
        int code = 100_000 + secureRandom.nextInt(900_000); // Generates a number between 100000 and 999999
        String codeStr = String.valueOf(code);
        codeCache.put(userUuid, codeStr);
        return codeStr;
    }

    /**
     * Verifies the provided code against the stored code for the given user ID.
     *
     * @param userId The unique identifier for the user.
     * @param code   The code to verify.
     * @return True if the code matches and is valid; false otherwise.
     */
    public boolean verifyCode(String userUuid, String code, boolean deactivateCode) {
        String cachedCode = codeCache.getIfPresent(userUuid);
        if (cachedCode != null && cachedCode.equals(code)) {
        	if(deactivateCode) {
        		codeCache.invalidate(userUuid); // Invalidate the code after successful verification
        	}
            return true;
        }
        return false;
    }
}


