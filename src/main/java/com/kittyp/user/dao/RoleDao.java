/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dao;

import com.kittyp.user.entity.Role;
import com.kittyp.user.enums.ERole;

/**
 * @author rrohan419@gmail.com 
 */
public interface RoleDao {

	Role roleByName(ERole role);
}
