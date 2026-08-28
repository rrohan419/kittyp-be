package com.kittyp.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-GCM encryption for sensitive columns (e.g. WhatsApp tokens).
 * Values are stored as {@code enc:v1:<base64(iv+ciphertext)>}. Plaintext legacy rows still read.
 */
@Slf4j
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "enc:v1:";
    private static final String LOCAL_FALLBACK = "kittyp-local-crypto-fallback";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private static volatile byte[] keyBytes;

    private final Environment environment;

    @Value("${app.crypto.secret:}")
    private String cryptoSecret;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    public EncryptedStringConverter(Environment environment) {
        this.environment = environment;
    }

    /** No-arg for JPA AttributeConverter instantiation in unit tests. */
    public EncryptedStringConverter() {
        this.environment = null;
    }

    @PostConstruct
    void init() {
        boolean localProfile = isLocalProfile();
        String secret = resolveSecret(localProfile);
        keyBytes = deriveKey(secret);
    }

    private String resolveSecret(boolean localProfile) {
        boolean blank = !StringUtils.hasText(cryptoSecret);
        boolean hardcodedFallback = LOCAL_FALLBACK.equals(cryptoSecret);
        boolean jwtFallback = StringUtils.hasText(jwtSecret) && jwtSecret.equals(cryptoSecret);

        if (!localProfile && (blank || hardcodedFallback || jwtFallback)) {
            throw new IllegalStateException(
                    "APP_CRYPTO_SECRET must be set to a dedicated secret (not JWT_SECRET or the local fallback) "
                            + "when spring.profiles.active is not 'local'.");
        }
        if (blank || hardcodedFallback) {
            log.warn("app.crypto.secret not set — using derived local fallback key. Set APP_CRYPTO_SECRET in production.");
            return LOCAL_FALLBACK;
        }
        if (jwtFallback) {
            log.warn("app.crypto.secret is falling back to jwt.secret — set a dedicated APP_CRYPTO_SECRET.");
        }
        return cryptoSecret;
    }

    private boolean isLocalProfile() {
        if (environment == null) {
            return true;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "local".equalsIgnoreCase(p));
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        if (attribute.startsWith(PREFIX)) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(requireKey(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt sensitive field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        if (!dbData.startsWith(PREFIX)) {
            // Legacy plaintext — still usable until next save re-encrypts
            return dbData;
        }
        try {
            byte[] all = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LEN];
            buf.get(iv);
            byte[] cipherText = new byte[buf.remaining()];
            buf.get(cipherText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(requireKey(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt sensitive field", e);
        }
    }

    private static byte[] requireKey() {
        byte[] k = keyBytes;
        if (k == null) {
            // Converter may run before @PostConstruct in tests — derive from default
            k = deriveKey(LOCAL_FALLBACK);
            keyBytes = k;
        }
        return k;
    }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot derive crypto key", e);
        }
    }
}
