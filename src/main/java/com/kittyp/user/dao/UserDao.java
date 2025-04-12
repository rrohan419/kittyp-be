package com.kittyp.user.dao;

import com.kittyp.user.entity.User;

public interface UserDao {

	User saveUser(User user);
		
	boolean userPresentByEmail(String email);
	
	User userByEmail(String email);
}
