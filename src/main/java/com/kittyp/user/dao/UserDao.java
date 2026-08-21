package com.kittyp.user.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.user.entity.User;

public interface UserDao {

	User saveUser(User user);
		
	boolean userPresentByEmail(String email);
	
	User userByEmail(String email);
	
	User userByUuid(String uuid);

	User userByPetUuid(String petUuid);
	
	Page<User> findAllUsers(Pageable pageable);

	Page<User> findPetOwnerUsers(String q, Pageable pageable);

	Integer countActiveUsers();
}
