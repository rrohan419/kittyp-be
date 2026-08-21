/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.kittyp.auth.util.JwtUtils;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.FcmPushNotificationService;
import com.kittyp.notification.service.SmsService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dao.UserFcmTokenDao;
import com.kittyp.user.dto.ProfileOtpSendRequest;
import com.kittyp.user.dto.ProfileOtpVerifyRequest;
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.models.FcmTokenModel;
import com.kittyp.user.models.PetModel;
import com.kittyp.user.models.UserDetailsModel;

import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
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
	private final FcmPushNotificationService fcmPushNotificationService;
	private final UserFcmTokenDao fcmTokenDao;
	private final JwtUtils jwtUtils;
	private final SmsService smsService;
	private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;

	@Transactional
	@Override
	public UserDetailsModel userDetailsByEmail(String email) {
		logger.info("Fetching user details for email: {}", email);
		User user = userDao.userByEmail(email);

		if (user == null) {
			logger.warn("No user found with email: {}", email);
			return null;
		}

		// Ensure clinic-registered pets appear after late signup / soft-link.
		clinicOwnerUserLinkService.linkUserToClinicOwners(user);
		user = userDao.userByEmail(email);

		UserDetailsModel userDetailsModel = toUserDetailsModel(user);

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

		boolean emailChanging = userDetailDto.getEmail() != null && !userDetailDto.getEmail().isBlank()
				&& !userDetailDto.getEmail().trim().equalsIgnoreCase(user.getEmail());
		boolean phoneChanging = isPhoneChanging(user, userDetailDto);

		if (phoneChanging) {
			String local = userDetailDto.getPhoneNumber() == null ? "" : userDetailDto.getPhoneNumber().trim();
			if (!local.matches("\\d{10}")) {
				throw new CustomException("Phone number must be exactly 10 digits", HttpStatus.BAD_REQUEST);
			}
		}

		if (emailChanging) {
			String newEmail = userDetailDto.getEmail().trim().toLowerCase();
			if (!newEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
				throw new CustomException("Invalid email address", HttpStatus.BAD_REQUEST);
			}
			if (userDao.userPresentByEmail(newEmail)) {
				throw new CustomException("Email is already in use", HttpStatus.CONFLICT);
			}
			if (!verificationCodeService.isVerified(
					VerificationCodeService.profileEmailVerifiedKey(user.getUuid(), newEmail))) {
				throw new CustomException("Email re-verification required before changing email",
						HttpStatus.BAD_REQUEST);
			}
			user.setEmail(newEmail);
			verificationCodeService.clearVerified(
					VerificationCodeService.profileEmailVerifiedKey(user.getUuid(), newEmail));
			logger.debug("Updated email to: {}", newEmail);
		}

		if (userDetailDto.getFirstName() != null && !userDetailDto.getFirstName().isBlank()) {
			user.setFirstName(userDetailDto.getFirstName());
			logger.debug("Updated first name to: {}", userDetailDto.getFirstName());
		}
		if (userDetailDto.getLastName() != null && !userDetailDto.getLastName().isBlank()) {
			user.setLastName(userDetailDto.getLastName());
			logger.debug("Updated last name to: {}", userDetailDto.getLastName());
		}

		if (phoneChanging) {
			String fullPhone = normalizePhone(userDetailDto.getPhoneCountryCode(), userDetailDto.getPhoneNumber());
			if (!verificationCodeService.isVerified(
					VerificationCodeService.profilePhoneVerifiedKey(user.getUuid(), fullPhone))) {
				throw new CustomException("Phone re-verification required before changing phone number",
						HttpStatus.BAD_REQUEST);
			}
			user.setPhoneCountryCode(userDetailDto.getPhoneCountryCode().trim());
			user.setPhoneNumber(userDetailDto.getPhoneNumber().trim());
			verificationCodeService.clearVerified(
					VerificationCodeService.profilePhoneVerifiedKey(user.getUuid(), fullPhone));
			logger.debug("Updated phone number to: {}", fullPhone);
		}

		if (userDetailDto.getAge() != null) {
			if (userDetailDto.getAge() < 1 || userDetailDto.getAge() > 120) {
				throw new CustomException("Age must be between 1 and 120", HttpStatus.BAD_REQUEST);
			}
			user.setAge(userDetailDto.getAge());
			logger.debug("Updated age to: {}", userDetailDto.getAge());
		}

		user = userDao.saveUser(user);
		if (emailChanging || phoneChanging) {
			clinicOwnerUserLinkService.linkUserToClinicOwners(user);
		}
		logger.info("User details updated successfully for email: {}", user.getEmail());
		UserDetailsModel model = toUserDetailsModel(user);
		if (emailChanging) {
			// Keep session alive after email change by issuing a fresh JWT for the new email
			model.setAccessToken(jwtUtils.generateTokenFromEmail(user.getEmail()));
		}
		return model;
	}

	private UserDetailsModel toUserDetailsModel(User user) {
		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
				.map(UserRole::getRole)
				.map(role -> role.getName().name())
				.collect(Collectors.toSet()));

		if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()
				&& (userDetailsModel.getPhoneCountryCode() == null || userDetailsModel.getPhoneCountryCode().isBlank())) {
			userDetailsModel.setPhoneCountryCode("+91");
		}

		// Parent visibility is membership via user_uuid only. Clinic soft-hide uses pet.isActive
		// and must not remove linked pets from the parent's My Pets view.
		if (user.getPets() != null && !user.getPets().isEmpty()) {
			Set<PetModel> petModels = user.getPets().stream()
					.map(pet -> mapper.convert(pet, PetModel.class))
					.collect(Collectors.toSet());
			userDetailsModel.setOwnerPets(petModels);
		} else {
			userDetailsModel.setOwnerPets(new HashSet<>());
		}
		return userDetailsModel;
	}

	@Override
	public MessageResponse sendProfileOtp(String authEmail, ProfileOtpSendRequest request) {
		User user = requireUserByEmail(authEmail);
		String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();

		if ("EMAIL".equals(channel)) {
			if (request.getEmail() == null || request.getEmail().isBlank()) {
				throw new CustomException("Email is required", HttpStatus.BAD_REQUEST);
			}
			String newEmail = request.getEmail().trim().toLowerCase();
			if (newEmail.equalsIgnoreCase(user.getEmail())) {
				throw new CustomException("New email must be different from current email", HttpStatus.BAD_REQUEST);
			}
			if (userDao.userPresentByEmail(newEmail)) {
				throw new CustomException("Email is already in use", HttpStatus.CONFLICT);
			}
			String code = verificationCodeService.generateCode(
					VerificationCodeService.profileEmailOtpKey(user.getUuid(), newEmail));
			zeptoMailService.sendSignupOtpEmail(newEmail, code, "EMAIL", null);
			return new MessageResponse("OTP sent to email");
		}

		if ("PHONE".equals(channel)) {
			if (request.getPhone() == null || request.getPhone().isBlank()) {
				throw new CustomException("Phone is required", HttpStatus.BAD_REQUEST);
			}
			String phone = request.getPhone().trim();
			// Expect country code + 10-digit local number, e.g. +919876543210
			String digits = phone.replaceAll("\\D", "");
			if (digits.length() < 10 || !digits.substring(digits.length() - 10).matches("\\d{10}")) {
				throw new CustomException("Phone number must include a valid 10-digit local number",
						HttpStatus.BAD_REQUEST);
			}
			String currentFull = normalizePhone(user.getPhoneCountryCode(), user.getPhoneNumber());
			if (phone.equals(currentFull)) {
				throw new CustomException("New phone must be different from current phone", HttpStatus.BAD_REQUEST);
			}
			String code = verificationCodeService.generateCode(
					VerificationCodeService.profilePhoneOtpKey(user.getUuid(), phone));
			smsService.sendOtp(phone, code);
			return new MessageResponse("OTP sent to phone");
		}

		throw new CustomException("channel must be EMAIL or PHONE", HttpStatus.BAD_REQUEST);
	}

	@Override
	public Map<String, Boolean> verifyProfileOtp(String authEmail, ProfileOtpVerifyRequest request) {
		User user = requireUserByEmail(authEmail);
		String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();
		boolean ok;

		if ("EMAIL".equals(channel)) {
			String newEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
			ok = verificationCodeService.verifyCode(
					VerificationCodeService.profileEmailOtpKey(user.getUuid(), newEmail), request.getCode(), true);
			if (ok) {
				verificationCodeService.markVerified(
						VerificationCodeService.profileEmailVerifiedKey(user.getUuid(), newEmail));
			}
		} else if ("PHONE".equals(channel)) {
			String phone = request.getPhone() == null ? "" : request.getPhone().trim();
			ok = verificationCodeService.verifyCode(
					VerificationCodeService.profilePhoneOtpKey(user.getUuid(), phone), request.getCode(), true);
			if (ok) {
				verificationCodeService.markVerified(
						VerificationCodeService.profilePhoneVerifiedKey(user.getUuid(), phone));
			}
		} else {
			throw new CustomException("channel must be EMAIL or PHONE", HttpStatus.BAD_REQUEST);
		}

		if (!ok) {
			throw new CustomException("Invalid or expired OTP", HttpStatus.BAD_REQUEST);
		}
		return Map.of("verified", true);
	}

	private User requireUserByEmail(String email) {
		User user = userDao.userByEmail(email);
		if (user == null) {
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}
		return user;
	}

	private static boolean isPhoneChanging(User user, UserDetailDto dto) {
		if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()
				|| dto.getPhoneCountryCode() == null || dto.getPhoneCountryCode().isBlank()) {
			return false;
		}
		String next = normalizePhone(dto.getPhoneCountryCode(), dto.getPhoneNumber());
		String currentCode = user.getPhoneCountryCode() == null || user.getPhoneCountryCode().isBlank()
				? "+91"
				: user.getPhoneCountryCode();
		String current = normalizePhone(currentCode, user.getPhoneNumber());
		return !next.equals(current);
	}

	private static String normalizePhone(String countryCode, String phoneNumber) {
		String code = countryCode == null ? "" : countryCode.trim();
		String number = phoneNumber == null ? "" : phoneNumber.trim();
		return code + number;
	}

	@Override
	public boolean updatePassword(UpdatePasswordDto updatePasswordDto) {
		User user;
		try {
			user = userDao.userByEmail(updatePasswordDto.getEmail());
		} catch (Exception e) {
			user = null;
		}

		if (user == null) {
			// Uniform response — do not reveal whether the email exists
			throw new CustomException("Invalid or expired reset code", HttpStatus.BAD_REQUEST);
		}

		boolean verified = verificationCodeService.verifyCode(user.getUuid(), updatePasswordDto.getCode(), true);

		if (verified) {
			user.setPassword(encoder.encode(updatePasswordDto.getPassword()));
			userDao.saveUser(user);
			logger.info("Password updated successfully for user UUID: {}", user.getUuid());
			return true;
		}

		throw new CustomException("Invalid or expired reset code", HttpStatus.BAD_REQUEST);
	}

	@Override
	public boolean sendResetPasswordCode(String email) {
		// Anti-enumeration: always succeed from the caller's perspective
		try {
			User user = userDao.userByEmail(email);
			if (user != null) {
				zeptoMailService.sendPasswordResetCode(user.getEmail());
			}
		} catch (Exception e) {
			logger.debug("Password reset requested for non-existent or invalid email");
		}
		return true;
	}

	@Override
	public boolean verifyResetPasswordCode(String code, String email) {
		User user;
		try {
			user = userDao.userByEmail(email);
		} catch (Exception e) {
			user = null;
		}

		if (user == null) {
			return false;
		}

		return verificationCodeService.verifyCode(user.getUuid(), code, false);
	}

	@Override
	public PaginationModel<UserDetailsModel> getAllUsers(Integer pageNumber, Integer pageSize, String q) {
		logger.info("Fetching users with pagination: page {}, size {}", pageNumber, pageSize);
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
		String query = q == null ? "" : q.trim();
		Page<User> userPage = userDao.findPetOwnerUsers(query, pageable);

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

	@Override
	public UserDetailsModel updateUserProfile(String userUuid, String profilePictureUrl) {
		User user = userDao.userByUuid(userUuid);
		if (user == null) {
			logger.warn("User not found with UUID: {}", userUuid);
			throw new CustomException("User not found", HttpStatus.NOT_FOUND);
		}

		user.setProfilePictureUrl(profilePictureUrl);
		user = userDao.saveUser(user);

		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
				.map(UserRole::getRole)
				.map(role -> role.getName().name())
				.collect(Collectors.toSet()));

		logger.info("User profile updated for UUID: {}", userUuid);
		return userDetailsModel;
	}

	@Override
	public FcmTokenModel updateUserFcmToken(String email, String fcmToken, HttpServletRequest request) {
    User user = userDao.userByEmail(email);
    if (user == null) {
        logger.warn("User not found with email: {}", email);
        throw new CustomException("User not found", HttpStatus.NOT_FOUND);
    }

    // Fetch all tokens for user
    List<UserFcmToken> userTokens = fcmTokenDao.findByUser(user);

    UserFcmToken currentToken = null;

    // Check if token already exists
    for (UserFcmToken token : userTokens) {
        if (token.getToken().equals(fcmToken)) {
            currentToken = token;
        }

        // Deactivate all tokens
        if (Boolean.TRUE.equals(token.getIsActive())) {
            token.setActive(Boolean.FALSE);
            fcmTokenDao.savFcmToken(token);
        }
    }

    // If token exists, just mark it active
    if (currentToken != null) {
        currentToken.setActive(Boolean.TRUE);
        fcmTokenDao.savFcmToken(currentToken);
        logger.info("Activated existing FCM token for user: {}", email);
        return new FcmTokenModel(currentToken.getToken());
    }

    // If token does not exist, create new
    UserAgent ua = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
    String browser = ua.getBrowser().getName();
    String os = ua.getOperatingSystem().getName();
    String deviceType = ua.getOperatingSystem().getDeviceType().getName();

    UserFcmToken newToken = UserFcmToken.builder()
            .user(user)
            .token(fcmToken)
            .active(Boolean.TRUE)
            .deviceType(deviceType)
            .deviceInfo(os + "-" + browser)
            .build();

    UserFcmToken savedToken = fcmTokenDao.savFcmToken(newToken);

    logger.info("Device Info: Browser={}, OS={}, DeviceType={}", browser, os, deviceType);
    logger.info("User FCM token saved and activated for email: {}", email);

    return new FcmTokenModel(savedToken.getToken());
}


	@Override
	public void sendPushNotification(String email, String title, String body) {
		User user = userDao.userByEmail(email);
		List<UserFcmToken> fcmTokens = fcmTokenDao.findByUser(user);
		if (fcmTokens != null && !fcmTokens.isEmpty()) {
			List<String> fcmTokensList = fcmTokens.stream().map(UserFcmToken::getToken).toList();
			fcmPushNotificationService.sendNotificationToUser(fcmTokensList, title, body);
		}
	}
}
