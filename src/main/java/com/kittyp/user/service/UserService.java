/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.service;

import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.models.UserDetailsModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface UserService {

	UserDetailsModel userDetailsByEmail(String email);
	
	void addRoleAdminToUser(String uuid);
	
	UserDetailsModel updateUserDetail(String email, UserDetailDto userDetailDto);

	boolean sendResetPasswordCode(String email);
	
	boolean verifyResetPasswordCode(String code, String email);
	
	boolean updatePassword(UpdatePasswordDto updatePasswordDto);
}
