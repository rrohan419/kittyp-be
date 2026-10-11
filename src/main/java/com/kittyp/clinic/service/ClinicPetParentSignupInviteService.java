package com.kittyp.clinic.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.ClinicPetParentSignupInvite;
import com.kittyp.clinic.repository.ClinicPetParentSignupInviteRepository;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicPetParentSignupInviteService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final long INVITE_TTL_HOURS = 24;

    private final ClinicPetParentSignupInviteRepository inviteRepository;

    @Transactional
    public String createInvite(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new CustomException("Invite email is required", HttpStatus.BAD_REQUEST);
        }

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        ClinicPetParentSignupInvite invite = ClinicPetParentSignupInvite.builder()
                .tokenHash(hashToken(token))
                .email(normalizedEmail)
                .expiresAt(LocalDateTime.now().plusHours(INVITE_TTL_HOURS))
                .build();
        inviteRepository.save(invite);
        return token;
    }

    @Transactional(readOnly = true)
    public String getInvitedEmail(String token) {
        if (!hasValidTokenFormat(token)) {
            throw invalidInvite();
        }
        ClinicPetParentSignupInvite invite = inviteRepository
                .findByTokenHashAndIsActiveTrue(hashToken(token))
                .filter(record -> record.getConsumedAt() == null && record.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(this::invalidInvite);
        return invite.getEmail();
    }

    @Transactional
    public boolean consumeInvite(String token, String email) {
        if (!hasValidTokenFormat(token)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return inviteRepository.consumeIfValid(hashToken(token), normalizeEmail(email), now) == 1;
    }

    private boolean hasValidTokenFormat(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private CustomException invalidInvite() {
        return new CustomException("This clinic invitation is invalid or expired", HttpStatus.BAD_REQUEST);
    }
}
