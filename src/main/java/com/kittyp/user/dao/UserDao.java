package com.kittyp.user.dao;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.user.entity.User;

public interface UserDao {

	User saveUser(User user);
		
	boolean userPresentByEmail(String email);
	
	User userByEmail(String email);
	
	User userByUuid(String uuid);

	User userByPetUuid(String petUuid);

	Optional<User> findOptionalByPetUuid(String petUuid);
	
	Page<User> findAllUsers(Pageable pageable);

	Page<User> findAllUsers(String q, Pageable pageable);

	Page<User> findPetOwnerUsers(String q, Pageable pageable);

	Integer countActiveUsers();

	/** Every account, matching the admin users list with an empty search. */
	long countAllUsers();

	long countCreatedSince(java.time.LocalDateTime since);

	java.util.List<Object[]> countCreatedByDay(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
