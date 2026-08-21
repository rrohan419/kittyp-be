/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.service;

import java.util.Map;

import com.kittyp.common.model.MessageResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.user.dto.ProfileOtpSendRequest;
import com.kittyp.user.dto.ProfileOtpVerifyRequest;
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.models.FcmTokenModel;
import com.kittyp.user.models.UserDetailsModel;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author rrohan419@gmail.com
 */
public interface UserService {

	UserDetailsModel userDetailsByEmail(String email);

	void addRoleAdminToUser(String uuid);

	UserDetailsModel updateUserDetail(String email, UserDetailDto userDetailDto);

	MessageResponse sendProfileOtp(String authEmail, ProfileOtpSendRequest request);

	Map<String, Boolean> verifyProfileOtp(String authEmail, ProfileOtpVerifyRequest request);

	boolean sendResetPasswordCode(String email);

	boolean verifyResetPasswordCode(String code, String email);

	boolean updatePassword(UpdatePasswordDto updatePasswordDto);

	PaginationModel<UserDetailsModel> getAllUsers(Integer pageNumber, Integer pageSize, String q);

	UserDetailsModel updateUserStatus(String userUuid, boolean enabled);

	UserDetailsModel updateUserProfile(String userUuid, String profilePictureUrl);

	FcmTokenModel updateUserFcmToken(String email, String fcmToken, HttpServletRequest request);

	void sendPushNotification(String email, String title, String body);
}
