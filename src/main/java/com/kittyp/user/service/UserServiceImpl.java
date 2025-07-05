/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.models.UserDetailsModel;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private final UserDao userDao;
	private final Mapper mapper;
	private final RoleDao roleDao;
	private final VerificationCodeService verificationCodeService;
	private final PasswordEncoder encoder;
	private final ZeptoMailService zeptoMailService;

	@Override
	public UserDetailsModel userDetailsByEmail(String email) {
		logger.info("Fetching user details for email: {}", email);
		User user = userDao.userByEmail(email);

		if (user == null) {
			logger.warn("No user found with email: {}", email);
			return null;
		}

		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
				.map(UserRole::getRole)
				.map(role -> role.getName().name())
				.collect(Collectors.toSet()));

		logger.info("User details retrieved successfully for email: {}", email);
		return userDetailsModel;
	}

	@Transactional
	@Override
	public void addRoleAdminToUser(String uuid) {
		logger.info("Adding ROLE_ADMIN to user with UUID: {}", uuid);
		User user = userDao.userByUuid(uuid);

		if (user == null) {
			logger.error("User not found for UUID: {}", uuid);
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		user.addRole(roleDao.roleByName(ERole.ROLE_ADMIN));
		userDao.saveUser(user);
		logger.info("ROLE_ADMIN successfully added to user UUID: {}", uuid);
	}

	@Override
	public UserDetailsModel updateUserDetail(String email, UserDetailDto userDetailDto) {
		logger.info("Updating user details for email: {}", email);
		User user = userDao.userByEmail(email);

		if (user == null) {
			logger.warn("No user found with email: {}", email);
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		if (userDetailDto.getEmail() != null && !userDetailDto.getEmail().isBlank()) {
			user.setEmail(userDetailDto.getEmail());
			logger.debug("Updated email to: {}", userDetailDto.getEmail());
		}
		if (userDetailDto.getFirstName() != null && !userDetailDto.getFirstName().isBlank()) {
			user.setFirstName(userDetailDto.getFirstName());
			logger.debug("Updated first name to: {}", userDetailDto.getFirstName());
		}
		if (userDetailDto.getLastName() != null && !userDetailDto.getLastName().isBlank()) {
			user.setLastName(userDetailDto.getLastName());
			logger.debug("Updated last name to: {}", userDetailDto.getLastName());
		}
		if (userDetailDto.getPhoneNumber() != null && !userDetailDto.getPhoneNumber().isBlank()
				&& userDetailDto.getPhoneCountryCode() != null && !userDetailDto.getPhoneCountryCode().isBlank()) {
			user.setPhoneCountryCode(userDetailDto.getPhoneCountryCode());
			user.setPhoneNumber(userDetailDto.getPhoneNumber());
			logger.debug("Updated phone number to: {}{}", userDetailDto.getPhoneCountryCode(), userDetailDto.getPhoneNumber());
		}

		user = userDao.saveUser(user);
		logger.info("User details updated successfully for email: {}", email);
		return mapper.convert(user, UserDetailsModel.class);
	}

	@Override
	public boolean updatePassword(UpdatePasswordDto updatePasswordDto) {
		logger.info("Updating password for email: {}", updatePasswordDto.getEmail());
		User user = userDao.userByEmail(updatePasswordDto.getEmail());

		if (user == null) {
			logger.warn("No user found with email: {}", updatePasswordDto.getEmail());
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		boolean verified = verificationCodeService.verifyCode(user.getUuid(), updatePasswordDto.getCode(), true);

		if (verified) {
			user.setPassword(encoder.encode(updatePasswordDto.getPassword()));
			userDao.saveUser(user);
			logger.info("Password updated successfully for user UUID: {}", user.getUuid());
			return true;
		}

		logger.warn("Verification code invalid or expired for user UUID: {}", user.getUuid());
		return false;
	}

	@Override
	public boolean sendResetPasswordCode(String email) {
		logger.info("Sending password reset code to email: {}", email);
		User user = userDao.userByEmail(email);

		if (user == null) {
			logger.warn("No user found with email: {}", email);
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		zeptoMailService.sendPasswordResetCode(user.getEmail());
		logger.info("Password reset code sent to email: {}", email);
		return true;
	}

	@Override
	public boolean verifyResetPasswordCode(String code, String email) {
		logger.info("Verifying reset password code for email: {}", email);
		User user = userDao.userByEmail(email);

		if (user == null) {
			logger.warn("No user found with email: {}", email);
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		boolean result = verificationCodeService.verifyCode(user.getUuid(), code, false);

		if (result) {
			logger.info("Reset password code verified successfully for email: {}", email);
		} else {
			logger.warn("Invalid reset code for email: {}", email);
		}

		return result;
	}

	@Override
	public PaginationModel<UserDetailsModel> getAllUsers(Integer pageNumber, Integer pageSize) {
		logger.info("Fetching users with pagination: page {}, size {}", pageNumber, pageSize);
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
		Page<User> userPage = userDao.findAllUsers(pageable);

		List<UserDetailsModel> userModels = userPage.getContent().stream()
			.map(user -> {
				UserDetailsModel model = mapper.convert(user, UserDetailsModel.class);
				model.setRoles(user.getUserRoles().stream()
					.map(UserRole::getRole)
					.map(role -> role.getName().name())
					.collect(Collectors.toSet()));
				return model;
			})
			.collect(Collectors.toList());

		logger.info("Total users fetched: {}", userModels.size());
		return userPageToModel(new PageImpl<>(userModels, pageable, userPage.getTotalElements()));
	}

	private PaginationModel<UserDetailsModel> userPageToModel(Page<UserDetailsModel> userPage) {
		PaginationModel<UserDetailsModel> paginationModel = new PaginationModel<>();
		paginationModel.setModels(userPage.getContent());
		paginationModel.setIsFirst(userPage.isFirst());
		paginationModel.setIsLast(userPage.isLast());
		paginationModel.setTotalElements(userPage.getTotalElements());
		paginationModel.setTotalPages(userPage.getTotalPages());
		return paginationModel;
	}

	@Transactional
	@Override
	public UserDetailsModel updateUserStatus(String userUuid, boolean enabled) {
		logger.info("Updating user status for UUID: {} to enabled: {}", userUuid, enabled);
		User user = userDao.userByUuid(userUuid);

		if (user == null) {
			logger.warn("User not found with UUID: {}", userUuid);
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		user.setEnabled(enabled);
		user.setIsActive(enabled);
		user = userDao.saveUser(user);

		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
				.map(UserRole::getRole)
				.map(role -> role.getName().name())
				.collect(Collectors.toSet()));

		logger.info("User status updated for UUID: {} to enabled: {}", userUuid, enabled);
		return userDetailsModel;
	}
}

