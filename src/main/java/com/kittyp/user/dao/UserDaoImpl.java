/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dao;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Repository
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {

	private final UserRepository userRepository;
	private final Environment env;

	@Override
	public User saveUser(User user) {
		try {
			return userRepository.save(user);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public boolean userPresentByEmail(String email) {
		try {
			return userRepository.existsByEmail(email);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public User userByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("email", "email", email));
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public User userByUuid(String uuid) {
		return userRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResourceNotFoundException("uuid", "uuid", uuid));
	
	}

	@Override
	public Page<User> findAllUsers(Pageable pageable) {
		try {
			return userRepository.findAll(pageable);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
