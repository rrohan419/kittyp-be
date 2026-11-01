package com.kittyp.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.auth.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// Row lock serialises concurrent rotations of the same token.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from RefreshToken t where t.tokenHash = :hash")
	Optional<RefreshToken> findByTokenHash(@Param("hash") String hash);

	@Modifying(flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
	int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

	@Modifying(flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
	int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

	@Modifying(flushAutomatically = true)
	@Query("delete from RefreshToken t where t.user.id = :userId and t.expiresAt < :now")
	int deleteExpiredForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
