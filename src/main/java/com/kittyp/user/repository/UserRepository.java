package com.kittyp.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByUuid(String uuid);

	Optional<User> findByUuid(String uuid);

	Optional<User> findByPets_Uuid(String petUuid);

	/** Match local 10-digit phone stored in phone_number (with or without extra digits). */
	@Query("""
			SELECT u FROM User u
			WHERE u.phoneNumber IS NOT NULL
			AND (
				u.phoneNumber = :digits
				OR u.phoneNumber LIKE CONCAT('%', :digits)
			)
			""")
	List<User> findByPhoneDigits(@Param("digits") String digits);

	Page<User> findAll(Pageable pageable);

	Integer countByIsActiveTrue();

	/**
	 * Live search of active pet-parent KittyP accounts (ROLE_USER only).
	 * Parameterized LIKE — caller must escape %/_ in {@code q}.
	 */
	@Query("""
			SELECT DISTINCT u FROM User u
			JOIN u.userRoles ur
			JOIN ur.role r
			WHERE (u.isActive IS NULL OR u.isActive = true)
			AND u.enabled = true
			AND r.name = com.kittyp.user.enums.ERole.ROLE_USER
			AND (
				LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
				OR COALESCE(u.phoneNumber, '') LIKE CONCAT('%', :q, '%')
				OR LOWER(u.uuid) LIKE LOWER(CONCAT('%', :q, '%'))
			)
			ORDER BY u.createdAt DESC
			""")
	List<User> searchActiveUsers(@Param("q") String q, Pageable pageable);

}
