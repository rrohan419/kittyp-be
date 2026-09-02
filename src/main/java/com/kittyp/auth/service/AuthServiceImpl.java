package com.kittyp.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kittyp.auth.config.UserDetailsImpl;
import com.kittyp.auth.dto.GoogleUserInfo;
import com.kittyp.auth.dto.SignupOtpSendRequest;
import com.kittyp.auth.dto.SignupOtpVerifyRequest;
import com.kittyp.auth.dto.SocialSso;
import com.kittyp.auth.util.JwtUtils;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.PublicSignupRequestDto;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.enums.SignupRole;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceAlreadyExistsException;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.service.SmsService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserDao userDao;
	private final PasswordEncoder encoder;
	private final RoleDao roleDao;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final ZeptoMailService zeptoMailService;
	private final GoogleOAuth2Service googleOAuth2Service;
	private final ClinicDao clinicDao;
	private final ClinicDoctorRepository clinicDoctorRepository;
	private final ClinicDoctorInviteRepository clinicDoctorInviteRepository;
	private final DoctorProfileDao doctorProfileDao;
	private final VerificationCodeService verificationCodeService;
	private final SmsService smsService;
	private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private final LoginRateLimiter loginRateLimiter;

	@Transactional
	@Override
	public MessageResponse register(PublicSignupRequestDto signupRequestDto) {
		SignupRole role = signupRequestDto.getRole() != null ? signupRequestDto.getRole() : SignupRole.USER;
		return switch (role) {
			case USER -> registerUser(signupRequestDto);
			case DOCTOR -> registerDoctor(signupRequestDto.toDoctorRequest());
			case CLINIC -> registerClinic(signupRequestDto.toClinicRequest());
		};
	}

	@Transactional
	@Override
	public MessageResponse registerUser(SignupRequestDto signupRequestDto) {

		if (userDao.userPresentByEmail(signupRequestDto.getEmail())) {
			throw new ResourceAlreadyExistsException("User", "email", signupRequestDto.getEmail());
		}

		// Create new user
		User user = User.builder()
				.email(signupRequestDto.getEmail()).password(encoder.encode(signupRequestDto.getPassword()))
				.firstName(signupRequestDto.getFirstName()).lastName(signupRequestDto.getLastName()).build();

		user = userDao.saveUser(user);

		// Pet-parent path only. Client SignupRole.DOCTOR/CLINIC is dispatched in register().
		// The legacy Set<String> roles field is ignored and cannot escalate privileges.
		Role userRole = roleDao.roleByName(ERole.ROLE_USER);
		if (userRole == null) {
			throw new CustomException("Default ROLE_USER not found", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		user.getUserRoles().add(new UserRole(user, userRole));
		user = userDao.saveUser(user);
		clinicOwnerUserLinkService.linkUserToClinicOwners(user);
		zeptoMailService.sendWelcomeEmailforParent(user.getFirstName(), user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	@Transactional
	@Override
	public MessageResponse registerDoctor(SignupDoctorRequestDto req) {
		if (userDao.userPresentByEmail(req.getEmail())) {
			throw new ResourceAlreadyExistsException("User", "email", req.getEmail());
		}
		if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
			throw new CustomException("Phone number is required", HttpStatus.BAD_REQUEST);
		}
		if (req.getRegistrationNumber() == null || req.getRegistrationNumber().isBlank()) {
			throw new CustomException("Veterinary registration number is required", HttpStatus.BAD_REQUEST);
		}
		if (req.getDegreeCertificateUrl() == null || req.getDegreeCertificateUrl().isBlank()
				|| req.getRegistrationCertificateUrl() == null || req.getRegistrationCertificateUrl().isBlank()) {
			throw new CustomException("Degree and registration certificate uploads are required", HttpStatus.BAD_REQUEST);
		}
		if (!verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()))) {
			throw new CustomException("Email OTP verification required", HttpStatus.BAD_REQUEST);
		}
		if (!verificationCodeService.isVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()))
				&& !verificationCodeService.isVerified(
						VerificationCodeService.phoneVerifiedKey("+91" + req.getPhoneNumber().trim()))) {
			throw new CustomException("Phone OTP verification required", HttpStatus.BAD_REQUEST);
		}

		User user = createUserWithRole(req, ERole.ROLE_DOCTOR);
		user.setPhoneNumber(req.getPhoneNumber());
		user.setPhoneCountryCode("+91");
		user = userDao.saveUser(user);

		// Doctor signup is a personal account for online consultation. Clinic name/address/photos
		// on the payload are ignored — clinics register and verify on their own path.
		ClinicDoctorInvite invite = null;
		if (req.getInviteToken() != null && !req.getInviteToken().isBlank()) {
			invite = clinicDoctorInviteRepository.findByToken(req.getInviteToken().trim())
					.orElseThrow(() -> new CustomException("Invalid clinic invite token", HttpStatus.BAD_REQUEST));
			if (invite.getStatus() != ClinicDoctorInviteStatus.PENDING
					|| invite.getExpiresAt().isBefore(LocalDateTime.now())) {
				throw new CustomException("Clinic invite is expired or no longer valid", HttpStatus.BAD_REQUEST);
			}
			if (!invite.getEmail().equalsIgnoreCase(req.getEmail())) {
				throw new CustomException("Signup email must match the invited email", HttpStatus.BAD_REQUEST);
			}
		}

		Clinic clinic = invite != null ? invite.getClinic() : null;

		String license = req.getLicenseNumber() != null && !req.getLicenseNumber().isBlank()
				? req.getLicenseNumber()
				: req.getRegistrationNumber();

		DoctorProfile profile = doctorProfileDao.save(DoctorProfile.builder()
				.user(user)
				.licenseNumber(license)
				.registrationNumber(req.getRegistrationNumber())
				.phoneNumber(req.getPhoneNumber())
				.specialization(req.getSpecialization())
				.experienceYears(req.getExperience())
				.bio(req.getProfessionalSummary())
				.photoUrl(req.getPhotoUrl())
				.degreeCertificateUrl(req.getDegreeCertificateUrl())
				.registrationCertificateUrl(req.getRegistrationCertificateUrl())
				.governmentIdUrl(req.getGovernmentIdUrl())
				.licenseDocumentUrl(req.getRegistrationCertificateUrl())
				.clinic(clinic)
				.currency("INR")
				.emailOtpVerified(true)
				.phoneOtpVerified(true)
				.checkEmailOtp(true)
				.checkMobileOtp(true)
				.status(DoctorStatus.DOCUMENTS_SUBMITTED)
				.submittedAt(LocalDateTime.now())
				.build());

		Clinic personal = provisionPersonalPractice(user, profile);
		if (profile.getClinic() == null && personal != null) {
			profile.setClinic(personal);
			profile = doctorProfileDao.save(profile);
		}

		if (invite != null) {
			clinicDoctorRepository.save(ClinicDoctor.builder()
					.clinic(invite.getClinic())
					.doctor(profile)
					.role("doctor")
					.isActive(true)
					.joinedAt(java.time.LocalDate.now())
					.build());
			invite.setStatus(ClinicDoctorInviteStatus.ACCEPTED);
			clinicDoctorInviteRepository.save(invite);
		}

		verificationCodeService.clearVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		verificationCodeService.clearVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()));

		zeptoMailService.sendWelcomeEmailforDoctor(user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	@Override
	public MessageResponse sendSignupOtp(SignupOtpSendRequest request) {
		String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();
		if ("EMAIL".equals(channel)) {
			if (request.getEmail() == null || request.getEmail().isBlank()) {
				throw new CustomException("Email is required", HttpStatus.BAD_REQUEST);
			}
			String email = request.getEmail().trim().toLowerCase();
			if (userDao.userPresentByEmail(email)) {
				throw new ResourceAlreadyExistsException("User", "email", email);
			}
			String code = verificationCodeService.generateCode(VerificationCodeService.emailOtpKey(email));
			System.out.println("code = " + code);
			zeptoMailService.sendSignupOtpEmail(email, code, "EMAIL", null);
			return new MessageResponse("OTP sent to email");
		}
		if ("PHONE".equals(channel)) {
			if (request.getPhone() == null || request.getPhone().isBlank()) {
				throw new CustomException("Phone is required", HttpStatus.BAD_REQUEST);
			}
			String phone = request.getPhone().trim();
			String digits = phone.replaceAll("\\D", "");
			if (digits.length() < 10 || !digits.substring(digits.length() - 10).matches("\\d{10}")) {
				throw new CustomException("Phone number must include a valid 10-digit local number",
						HttpStatus.BAD_REQUEST);
			}
			String code = verificationCodeService.generateCode(VerificationCodeService.phoneOtpKey(phone));
			System.out.println("code = " + code);
			smsService.sendOtp(phone, code);
			return new MessageResponse("OTP sent to phone");
		}
		throw new CustomException("channel must be EMAIL or PHONE", HttpStatus.BAD_REQUEST);
	}

	@Override
	public Map<String, Boolean> verifySignupOtp(SignupOtpVerifyRequest request) {
		String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();
		boolean ok;
		System.out.println("request.getCode() = " + request.getCode());
		
		if ("EMAIL".equals(channel)) {
			String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
			ok = verificationCodeService.verifyCode(VerificationCodeService.emailOtpKey(email), request.getCode(), true);
			if (ok) {
				verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(email));
			}
		} else if ("PHONE".equals(channel)) {
			String phone = request.getPhone() == null ? "" : request.getPhone().trim();
			ok = verificationCodeService.verifyCode(VerificationCodeService.phoneOtpKey(phone), request.getCode(), true);
			if (ok) {
				verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey(phone));
				// Also mark digits-only / +91 forms so registerDoctor phoneNumber checks match
				String digits = phone.replaceAll("\\D", "");
				if (digits.length() >= 10) {
					String local10 = digits.substring(digits.length() - 10);
					verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey(local10));
					verificationCodeService.markVerified(VerificationCodeService.phoneVerifiedKey("+91" + local10));
				}
			}
		} else {
			throw new CustomException("channel must be EMAIL or PHONE", HttpStatus.BAD_REQUEST);
		}
		System.out.println("ok? = " + ok);
		if (!ok) {
			throw new CustomException("Invalid or expired OTP", HttpStatus.BAD_REQUEST);
		}
		return Map.of("verified", true);
	}

	@Transactional
	@Override
	public MessageResponse registerClinic(SignupClinicRequestDto signupClinicRequestDto) {
		if (userDao.userPresentByEmail(signupClinicRequestDto.getEmail())) {
			throw new ResourceAlreadyExistsException("User", "email", signupClinicRequestDto.getEmail());
		}
		if (signupClinicRequestDto.getClinicName() == null || signupClinicRequestDto.getClinicName().isBlank()) {
			throw new CustomException("Clinic name is required", HttpStatus.BAD_REQUEST);
		}
		if (!verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(signupClinicRequestDto.getEmail()))) {
			throw new CustomException("Email OTP verification required", HttpStatus.BAD_REQUEST);
		}

		User user = createUserWithRole(signupClinicRequestDto, ERole.ROLE_CLINIC_ADMIN);

		clinicDao.saveClinic(Clinic.builder()
				.name(signupClinicRequestDto.getClinicName())
				.licenseNumber(signupClinicRequestDto.getLicenseNumber())
				.address(signupClinicRequestDto.getAddress())
				.phone(signupClinicRequestDto.getPhone())
				.timezone(signupClinicRequestDto.getTimezone())
				.email(user.getEmail())
				.owner(user)
				.status(ClinicStatus.PENDING)
				.build());

		verificationCodeService.clearVerified(VerificationCodeService.emailVerifiedKey(signupClinicRequestDto.getEmail()));
		zeptoMailService.sendWelcomeEmailforClinicAdmin(user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	private Clinic provisionPersonalPractice(User user, DoctorProfile profile) {
		if (user.getId() != null) {
			for (Clinic owned : clinicDao.findAllByOwnerUserId(user.getId())) {
				if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(owned.getId(),
						user.getId())) {
					return owned;
				}
			}
		}
		String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
		String last = user.getLastName() == null ? "" : user.getLastName().trim();
		String full = (first + " " + last).trim();
		String name = full.isEmpty() ? "Personal practice" : "Dr. " + full;
		Clinic personal = clinicDao.saveClinic(Clinic.builder()
				.name(name)
				.email(user.getEmail())
				.phone(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : user.getPhoneNumber())
				.owner(user)
				.status(ClinicStatus.VERIFIED)
				.build());
		clinicDoctorRepository.save(ClinicDoctor.builder()
				.clinic(personal)
				.doctor(profile)
				.role("owner")
				.isActive(true)
				.joinedAt(java.time.LocalDate.now())
				.build());
		return personal;
	}

	private User createUserWithRole(SignupRequestDto signupRequestDto, ERole roleName) {
		User user = User.builder()
				.email(signupRequestDto.getEmail())
				.password(encoder.encode(signupRequestDto.getPassword()))
				.firstName(signupRequestDto.getFirstName())
				.lastName(signupRequestDto.getLastName())
				.build();

		Role role = roleDao.roleByName(roleName);
		user.addRole(role);
		return userDao.saveUser(user);
	}

	@Override
	public JwtResponseModel loginUser(LoginRequestDto loginRequestDto, String clientIp) {
		loginRateLimiter.assertAllowed(clientIp, loginRequestDto.getEmail());
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

			// Late clinic CRM link: parent may have been registered at a clinic months ago.
			try {
				User user = userDao.userByEmail(userDetails.getEmail());
				if (user != null) {
					clinicOwnerUserLinkService.linkUserToClinicOwners(user);
				}
			} catch (Exception ignored) {
				// Login must not fail if linking has an edge-case conflict.
			}

			loginRateLimiter.clear(clientIp, loginRequestDto.getEmail());
			return new JwtResponseModel(jwt, userDetails.getId(), userDetails.getUuid(), userDetails.getEmail(), roles);
		} catch (org.springframework.security.core.AuthenticationException ex) {
			loginRateLimiter.recordFailure(clientIp, loginRequestDto.getEmail());
			throw ex;
		}
	}

	@Override
	@Transactional
	public JwtResponseModel googleUserSignin(SocialSso socialSso) {
		try {
			// Get user info directly from Google using access token
			GoogleUserInfo googleUserInfo = googleOAuth2Service.getUserInfo(socialSso.getToken());
			
			// Check if user exists by email
			User existingUser = null;
			try {
				existingUser = userDao.userByEmail(googleUserInfo.getEmail());
			} catch (Exception e) {
				// User doesn't exist, will create new one
			}
			
			if (existingUser == null) {
				// Create new user
				existingUser = User.builder()
						.email(googleUserInfo.getEmail())
						.firstName(googleUserInfo.getGivenName())
						.lastName(googleUserInfo.getFamilyName())
						.password(encoder.encode(UUID.randomUUID().toString())) // Generate random password
						.enabled(true)
						.build();
				
				// Assign default role
				Role userRole = roleDao.roleByName(ERole.ROLE_USER);
				if (userRole == null) {
					throw new RuntimeException("Error: Default ROLE_USER not found.");
				}
				
				existingUser.addRole(userRole);
				existingUser = userDao.saveUser(existingUser);
				
				// Send welcome email
				zeptoMailService.sendWelcomeEmailforParent(existingUser.getFirstName(), existingUser.getEmail());
			}

			try {
				clinicOwnerUserLinkService.linkUserToClinicOwners(existingUser);
			} catch (Exception ignored) {
				// Do not fail Google sign-in on link edge cases
			}
			
			// Create authentication token
			UserDetailsImpl userDetails = (UserDetailsImpl) UserDetailsImpl.build(existingUser);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);
			
			List<String> roles = userDetails.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.toList();
			
			return new JwtResponseModel(jwt, userDetails.getId(), userDetails.getUuid(), userDetails.getEmail(), roles);
			
		} catch (Exception e) {
			throw new RuntimeException("Google authentication failed: " + e.getMessage(), e);
		}
	}

}
