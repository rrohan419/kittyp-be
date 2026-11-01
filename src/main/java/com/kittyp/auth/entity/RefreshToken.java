package com.kittyp.auth.entity;

import java.time.Instant;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Server-side record of an issued refresh token. Only the SHA-256 hash of the
 * token is stored. Tokens sharing a {@code familyId} descend from one login.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
		@Index(name = "idx_refresh_tokens_family", columnList = "family_id"),
		@Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "family_id", nullable = false, length = 36)
	private String familyId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	/** Set when this token was exchanged for a new one. */
	@Column(name = "rotated_at")
	private Instant rotatedAt;

	/** Set on logout, password change or detected token reuse. */
	@Column(name = "revoked_at")
	private Instant revokedAt;
}
