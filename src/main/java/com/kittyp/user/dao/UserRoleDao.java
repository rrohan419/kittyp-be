/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dao;

import java.util.List;
import java.util.Set;

import com.kittyp.user.entity.UserRole;

/**
 * @author rrohan419@gmail.com 
 */
public interface UserRoleDao {

	List<UserRole> saveAllUserRole(Set<UserRole> userRoles);
}
