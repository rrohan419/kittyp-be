package com.kittyp.auth.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kittyp.auth.config.UserDetailsImpl;
import com.kittyp.auth.dto.ActivateRoleRequest;
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
import com.kittyp.notification.service.Msg91OtpVerifyService;
import com.kittyp.notification.service.WhatsappOtpService;
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

	private final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
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
	private final WhatsappOtpService whatsappOtpService;
	private final Msg91OtpVerifyService msg91OtpVerifyService;
	private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private final LoginRateLimiter loginRateLimiter;
	private final UserDetailsService userDetailsService;
	private final RoleCredentialService roleCredentialService;
	private final RoleActivationFacade roleActivationFacade;

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

		rejectExistingEmail(signupRequestDto.getEmail());

		// Create new user
		User user = User.builder()
				.email(signupRequestDto.getEmail()).password(encoder.encode(signupRequestDto.getPassword()))
				.firstName(signupRequestDto.getFirstName()).lastName(signupRequestDto.getLastName()).build();

		user = userDao.saveUser(user);

		// Pet-parent path only. Client SignupRole.DOCTOR/CLINIC is dispatched in
		// register().
		// The legacy Set<String> roles field is ignored and cannot escalate privileges.
		Role userRole = roleDao.roleByName(ERole.ROLE_USER);
		if (userRole == null) {
			throw new CustomException("Default ROLE_USER not found", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		user.getUserRoles().add(new UserRole(user, userRole));
		user = userDao.saveUser(user);
		clinicOwnerUserLinkService.linkUserToClinicOwners(user);
		roleCredentialService.storeNewRolePassword(user, ERole.ROLE_USER, signupRequestDto.getPassword());
		zeptoMailService.sendWelcomeEmailforParent(user.getFirstName(), user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	@Transactional
	@Override
	public MessageResponse registerDoctor(SignupDoctorRequestDto req) {
		if (userDao.userPresentByEmail(req.getEmail())) {
			return enrollExistingDoctor(req);
		}
		SignupRequirements.requireDoctor(req, verificationCodeService, true);

		User user = createUserWithRole(req, ERole.ROLE_DOCTOR);
		user.setPhoneNumber(req.getPhoneNumber());
		user.setPhoneCountryCode("+91");
		user = userDao.saveUser(user);
		provisionNewDoctor(user, req);
		roleCredentialService.storeNewRolePassword(user, ERole.ROLE_DOCTOR, req.getPassword());

		zeptoMailService.sendWelcomeEmailforDoctor(user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	/**
	 * Adds the doctor role to an existing account only after the signup email OTP
	 * is verified and the role is not already present. Does not create a second
	 * profile or change users.password.
	 */
	private MessageResponse enrollExistingDoctor(SignupDoctorRequestDto req) {
		User existing = userDao.userByEmail(req.getEmail());
		if (hasRole(existing, ERole.ROLE_DOCTOR)
				|| !verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()))) {
			throw ResourceAlreadyExistsException.signInToContinue();
		}
		SignupRequirements.requireDoctor(req, verificationCodeService, true);
		ActivateRoleRequest activation = new ActivateRoleRequest();
		activation.setRole(SignupRole.DOCTOR);
		activation.setEmail(req.getEmail());
		activation.setPhoneNumber(req.getPhoneNumber());
		activation.setLicenseNumber(req.getLicenseNumber());
		activation.setRegistrationNumber(req.getRegistrationNumber());
		activation.setSpecialization(req.getSpecialization());
		activation.setExperience(req.getExperience());
		activation.setProfessionalSummary(req.getProfessionalSummary());
		activation.setDegreeCertificateUrl(req.getDegreeCertificateUrl());
		activation.setRegistrationCertificateUrl(req.getRegistrationCertificateUrl());
		activation.setGovernmentIdUrl(req.getGovernmentIdUrl());
		activation.setPhotoUrl(req.getPhotoUrl());
		activation.setInviteToken(req.getInviteToken());
		activation.setRolePassword(req.getPassword());
		return activateMissingRole(existing.getEmail(), activation);
	}

	/**
	 * Creates the doctor profile and personal practice for an existing user.
	 * Does not change the user's password or send email.
	 */
	public void provisionNewDoctor(User user, SignupDoctorRequestDto req) {
		// Doctor signup is a personal account for online consultation. Clinic
		// name/address/photos
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
	}

	@Override
	public MessageResponse sendSignupOtp(SignupOtpSendRequest request) {
		String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();
		if ("EMAIL".equals(channel)) {
			if (request.getEmail() == null || request.getEmail().isBlank()) {
				throw new CustomException("Email is required", HttpStatus.BAD_REQUEST);
			}
			String email = request.getEmail().trim().toLowerCase();
			String code = verificationCodeService.generateCode(VerificationCodeService.emailOtpKey(email));
			zeptoMailService.sendSignupOtpEmail(email, code, "EMAIL", null);
			return new MessageResponse("OTP sent to email");
		}
		if ("WHATSAPP".equals(channel)) {
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
			whatsappOtpService.sendOtp(digits, code);
			return new MessageResponse("OTP sent to whatsapp");
		}
		
		throw new CustomException("channel must be EMAIL, WHATSAPP", HttpStatus.BAD_REQUEST);
	}

	@Override
	public Map<String, Boolean> verifySignupOtp(SignupOtpVerifyRequest request) {
		String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();
		boolean ok;
		System.out.println("request.getCode() = " + request.getCode());

		if ("EMAIL".equals(channel)) {
			String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
			ok = verificationCodeService.verifyCode(VerificationCodeService.emailOtpKey(email), request.getCode(),
					true);
			if (ok) {
				verificationCodeService.markVerified(VerificationCodeService.emailVerifiedKey(email));
			}
		} else if ("PHONE".equals(channel)) {
			String phone = request.getPhone() == null ? "" : request.getPhone().trim();
			ok = msg91OtpVerifyService.verifyOtp(request.getAccessToken());
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
		} else if ("WHATSAPP".equals(channel)) {
			String phone = request.getPhone() == null ? "" : request.getPhone().trim();
			ok = verificationCodeService.verifyCode(VerificationCodeService.phoneOtpKey(phone), request.getCode(),
					true);
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
			throw new CustomException("channel must be EMAIL, WHATSAPP, or PHONE", HttpStatus.BAD_REQUEST);
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
			return enrollExistingClinic(signupClinicRequestDto);
		}
		SignupRequirements.requireClinic(signupClinicRequestDto, verificationCodeService, true);

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

		roleCredentialService.storeNewRolePassword(user, ERole.ROLE_CLINIC_ADMIN, signupClinicRequestDto.getPassword());
		verificationCodeService
				.clearVerified(VerificationCodeService.emailVerifiedKey(signupClinicRequestDto.getEmail()));
		zeptoMailService.sendWelcomeEmailforClinicAdmin(user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	private MessageResponse enrollExistingClinic(SignupClinicRequestDto req) {
		User existing = userDao.userByEmail(req.getEmail());
		if (hasRole(existing, ERole.ROLE_CLINIC_ADMIN)
				|| !verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()))) {
			throw ResourceAlreadyExistsException.signInToContinue();
		}
		SignupRequirements.requireClinic(req, verificationCodeService, true);
		ActivateRoleRequest activation = new ActivateRoleRequest();
		activation.setRole(SignupRole.CLINIC);
		activation.setEmail(req.getEmail());
		activation.setClinicName(req.getClinicName());
		activation.setLicenseNumber(req.getLicenseNumber());
		activation.setAddress(req.getAddress());
		activation.setPhone(req.getPhone());
		activation.setTimezone(req.getTimezone());
		activation.setRolePassword(req.getPassword());
		return activateMissingRole(existing.getEmail(), activation);
	}

	private MessageResponse activateMissingRole(String email, ActivateRoleRequest activation) {
		MessageResponse response = roleActivationFacade.activate(email, activation);
		if ("This role is already on your account.".equals(response.getMessage())) {
			throw ResourceAlreadyExistsException.signInToContinue();
		}
		return response;
	}

	private static boolean hasRole(User user, ERole role) {
		if (user.getUserRoles() == null) {
			return false;
		}
		for (UserRole userRole : user.getUserRoles()) {
			if (userRole.getRole() != null && role == userRole.getRole().getName()) {
				return true;
			}
		}
		return false;
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

	private void rejectExistingEmail(String email) {
		if (userDao.userPresentByEmail(email)) {
			throw ResourceAlreadyExistsException.signInToContinue();
		}
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
			UserDetailsImpl userDetails = loadLoginUser(loginRequestDto.getEmail());
			String rawPassword = loginRequestDto.getPassword();
			boolean accountMatch = rawPassword != null && encoder.matches(rawPassword, userDetails.getPassword());
			List<ERole> roleHits = assignedRoleHits(userDetails.getAuthorities(),
					roleCredentialService.matchingRoles(userDetails.getId(), rawPassword));
			RolePasswordMatch.Decision decision = RolePasswordMatch.decide(accountMatch, roleHits);
			if (decision.kind() == RolePasswordMatch.Kind.REJECT) {
				throw new BadCredentialsException("Bad credentials");
			}

			Authentication authentication = accountMatch
					? authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
							loginRequestDto.getEmail(), rawPassword))
					: new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

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
			JwtResponseModel response = new JwtResponseModel(jwt, userDetails.getId(), userDetails.getUuid(),
					userDetails.getEmail(), roles);
			if (decision.loginRole() != null) {
				response.setLoginRole(decision.loginRole().name());
			}
			return response;
		} catch (org.springframework.security.core.AuthenticationException ex) {
			loginRateLimiter.recordFailure(clientIp, loginRequestDto.getEmail());
			throw ex;
		}
	}

	/**
	 * Drops credential hits for roles no longer on the account. Stale rows stay in the database.
	 */
	static List<ERole> assignedRoleHits(Collection<? extends GrantedAuthority> authorities, List<ERole> roleHits) {
		if (roleHits == null || roleHits.isEmpty() || authorities == null || authorities.isEmpty()) {
			return List.of();
		}
		Set<String> held = new HashSet<>();
		for (GrantedAuthority authority : authorities) {
			if (authority != null && authority.getAuthority() != null) {
				held.add(authority.getAuthority());
			}
		}
		return roleHits.stream().filter(role -> role != null && held.contains(role.name())).toList();
	}

	private UserDetailsImpl loadLoginUser(String login) {
		try {
			return (UserDetailsImpl) userDetailsService.loadUserByUsername(login);
		} catch (UsernameNotFoundException ex) {
			throw new BadCredentialsException("Bad credentials");
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
