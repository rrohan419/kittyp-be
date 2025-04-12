/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.models.UserDetailsModel;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private final Mapper mapper;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public UserDetailsModel userDetailsByEmail(String email) {
		User user = userDao.userByEmail(email);
		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));
		
		return userDetailsModel;
//		return UserDetailsModel.builder()
//                .id(user.getId())
//                .email(user.getEmail())
//                .roles(user.getUserRoles().stream()
//                        .map(UserRole::getRole)
//                        .map(role -> role.getName().name())
//                        .collect(Collectors.toSet()))
//                .enabled(user.isEnabled())
//                .build();
	}

}
