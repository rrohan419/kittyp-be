/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dao;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.Role;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Repository
@RequiredArgsConstructor
public class RoleDaoImpl implements RoleDao {

	private final RoleRepository roleRepository;
	private final Environment env;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Role roleByName(ERole role) {
		return roleRepository.findByName(role)
				.orElseThrow(() -> new CustomException(env.getProperty(ExceptionConstant.ERROR_ROLE_NOT_FOUND),
						HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND));
	}

}
