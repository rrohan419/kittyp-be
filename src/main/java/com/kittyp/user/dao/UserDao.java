package com.kittyp.user.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.user.entity.User;

public interface UserDao {

	User saveUser(User user);
		
	boolean userPresentByEmail(String email);
	
	User userByEmail(String email);
	
	User userByUuid(String uuid);
	
	Page<User> findAllUsers(Pageable pageable);
}
