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

}
