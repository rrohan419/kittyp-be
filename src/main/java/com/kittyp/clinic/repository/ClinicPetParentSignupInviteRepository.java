package com.kittyp.clinic.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicPetParentSignupInvite;

public interface ClinicPetParentSignupInviteRepository
        extends JpaRepository<ClinicPetParentSignupInvite, Long> {

    Optional<ClinicPetParentSignupInvite> findByTokenHashAndIsActiveTrue(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ClinicPetParentSignupInvite invite
            set invite.consumedAt = :consumedAt
            where invite.tokenHash = :tokenHash
              and lower(invite.email) = :email
              and invite.consumedAt is null
              and invite.expiresAt > :consumedAt
              and invite.isActive = true
            """)
    int consumeIfValid(@Param("tokenHash") String tokenHash, @Param("email") String email,
            @Param("consumedAt") LocalDateTime consumedAt);
}
