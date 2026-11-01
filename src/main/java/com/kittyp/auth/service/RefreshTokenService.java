package com.kittyp.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kittyp.auth.entity.RefreshToken;
import com.kittyp.auth.repository.RefreshTokenRepository;
import com.kittyp.auth.util.JwtUtils;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Issues, rotates and revokes server-tracked refresh tokens.
 * Each refresh token is single-use; presenting an already-used token after the
 * grace window revokes the whole login family.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	/** Lets parallel tabs refresh with the same cookie without killing the session. */
	private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

	private final RefreshTokenRepository repository;
	private final JwtUtils jwtUtils;

	/**
	 * @param refreshToken new refresh token to send to the client, or {@code null}
	 *                     when the caller already received it from a concurrent
	 *                     refresh and must keep its current one.
	 */
	public record Rotation(User user, String refreshToken) {
	}

	@Transactional
	public String issue(User user) {
		return create(user, UUID.randomUUID().toString());
	}

	@Transactional(noRollbackFor = CustomException.class)
	public Rotation rotate(String rawToken) {
		if (!StringUtils.hasText(rawToken) || !jwtUtils.isRefreshToken(rawToken)) {
			throw invalid();
		}

		RefreshToken stored = repository.findByTokenHash(hash(rawToken)).orElseThrow(RefreshTokenService::invalid);
		Instant now = Instant.now();

		if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(now)) {
			throw invalid();
		}

		if (stored.getRotatedAt() != null) {
			if (stored.getRotatedAt().plus(REUSE_GRACE).isAfter(now) && isUserActive(stored.getUser())) {
				return new Rotation(stored.getUser(), null);
			}
			repository.revokeFamily(stored.getFamilyId(), now);
			throw invalid();
		}

		User user = stored.getUser();
		if (!isUserActive(user)) {
			repository.revokeFamily(stored.getFamilyId(), now);
			throw invalid();
		}

		stored.setRotatedAt(now);
		repository.save(stored);
		return new Rotation(user, create(user, stored.getFamilyId()));
	}

	/** Logout: revoke the login the given token belongs to. Never throws. */
	@Transactional
	public void revoke(String rawToken) {
		if (!StringUtils.hasText(rawToken)) {
			return;
		}
		repository.findByTokenHash(hash(rawToken))
				.ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
	}

	@Transactional
	public void revokeAllForUser(Long userId) {
		repository.revokeAllForUser(userId, Instant.now());
	}

	private String create(User user, String familyId) {
		Instant now = Instant.now();
		repository.deleteExpiredForUser(user.getId(), now);

		String raw = jwtUtils.generateRefreshToken(user.getEmail());
		repository.save(RefreshToken.builder()
				.user(user)
				.tokenHash(hash(raw))
				.familyId(familyId)
				.expiresAt(now.plusMillis(jwtUtils.getRefreshTokenExpirationMs()))
				.build());
		return raw;
	}

	private static boolean isUserActive(User user) {
		return user != null && !Boolean.FALSE.equals(user.getIsActive());
	}

	private static CustomException invalid() {
		return new CustomException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
