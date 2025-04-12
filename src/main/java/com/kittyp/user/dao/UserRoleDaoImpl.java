/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dao;

import java.util.List;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Repository
@RequiredArgsConstructor
public class UserRoleDaoImpl implements UserRoleDao {

	private final UserRoleRepository userRoleRepository;
	private final Environment env;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public List<UserRole> saveAllUserRole(Set<UserRole> userRoles) {
		try {
			return userRoleRepository.saveAll(userRoles);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
