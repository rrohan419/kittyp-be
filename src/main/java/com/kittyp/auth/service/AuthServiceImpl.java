package com.kittyp.auth.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceAlreadyExistsException;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.email.service.ZeptoMailService;
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
	private final DoctorProfileDao doctorProfileDao;
	private final VerificationCodeService verificationCodeService;

	@Transactional
	@Override
	public MessageResponse registerUser(SignupRequestDto signupRequestDto) {

		if (userDao.userPresentByEmail(signupRequestDto.getEmail())) {
			throw new ResourceAlreadyExistsException("User", "email", signupRequestDto.getEmail());
		}

		// Create new user
		User user = User.builder()
				.uuid(UUID.randomUUID().toString())
				.email(signupRequestDto.getEmail()).password(encoder.encode(signupRequestDto.getPassword()))
				.firstName(signupRequestDto.getFirstName()).lastName(signupRequestDto.getLastName()).build();

		user = userDao.saveUser(user);

		// Assign roles
		Set<String> strRoles = signupRequestDto.getRoles();
		Set<UserRole> userRoles = new HashSet<>();

		if (strRoles == null || strRoles.isEmpty()) {
			Role userRole = roleDao.roleByName(ERole.ROLE_USER);
			if (userRole == null) {
				throw new RuntimeException("Error: Default ROLE_USER not found.");
			}
			userRoles.add(new UserRole(user, userRole));
		} else {
			for (String role : strRoles) {
				Role foundRole;
				switch (role.toLowerCase()) {
                    case "admin":
                    case "admin_user":
                        foundRole = roleDao.roleByName(ERole.ROLE_ADMIN);
                        break;
                    case "mod":
                        foundRole = roleDao.roleByName(ERole.ROLE_MODERATOR);
                        break;
                    case "parent_user":
                    case "parent":
                        foundRole = roleDao.roleByName(ERole.ROLE_USER);
                        break;
                    case "doctor_user":
                    case "doctor":
                        foundRole = roleDao.roleByName(ERole.ROLE_DOCTOR);
                        break;
                    case "clinic_admin":
                        foundRole = roleDao.roleByName(ERole.ROLE_CLINIC_ADMIN);
                        break;
					case "clinic_staff":
						foundRole = roleDao.roleByName(ERole.ROLE_CLINIC_STAFF);
						break;
                    default:
                        foundRole = roleDao.roleByName(ERole.ROLE_USER);
                }
				userRoles.add(new UserRole(user, foundRole));
			}
		}

		user.getUserRoles().addAll(userRoles);
		userDao.saveUser(user);
		zeptoMailService.sendWelcomeEmail(user.getEmail());
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
		if (!verificationCodeService.isVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()))) {
			throw new CustomException("Phone OTP verification required", HttpStatus.BAD_REQUEST);
		}

		User user = createUserWithRole(req, ERole.ROLE_DOCTOR);
		user.setPhoneNumber(req.getPhoneNumber());
		user = userDao.saveUser(user);

		Clinic clinic = null;
		if (req.getClinicName() != null && !req.getClinicName().isBlank()) {
			clinic = clinicDao.saveClinic(Clinic.builder()
					.uuid(UUID.randomUUID().toString())
					.name(req.getClinicName())
					.licenseNumber(req.getRegistrationNumber())
					.address(req.getClinicAddress())
					.email(user.getEmail())
					.phone(req.getPhoneNumber())
					.owner(user)
					.status(ClinicStatus.PENDING)
					.build());
		}

		String license = req.getLicenseNumber() != null && !req.getLicenseNumber().isBlank()
				? req.getLicenseNumber()
				: req.getRegistrationNumber();

		doctorProfileDao.save(DoctorProfile.builder()
				.uuid(UUID.randomUUID().toString())
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
				.clinicPhotosUrls(req.getClinicPhotosUrls())
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

		verificationCodeService.clearVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()));
		verificationCodeService.clearVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()));

		zeptoMailService.sendWelcomeEmail(user.getEmail());
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
			zeptoMailService.sendSignupOtpEmail(email, code, "EMAIL", null);
			return new MessageResponse("OTP sent to email");
		}
		if ("PHONE".equals(channel)) {
			if (request.getPhone() == null || request.getPhone().isBlank()) {
				throw new CustomException("Phone is required", HttpStatus.BAD_REQUEST);
			}
			if (request.getEmail() == null || request.getEmail().isBlank()) {
				throw new CustomException("Email is required to deliver phone OTP", HttpStatus.BAD_REQUEST);
			}
			String phone = request.getPhone().trim();
			String email = request.getEmail().trim().toLowerCase();
			String code = verificationCodeService.generateCode(VerificationCodeService.phoneOtpKey(phone));
			zeptoMailService.sendSignupOtpEmail(email, code, "PHONE", phone);
			return new MessageResponse("OTP sent for phone verification (via email)");
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

		User user = createUserWithRole(signupClinicRequestDto, ERole.ROLE_CLINIC_ADMIN);

		clinicDao.saveClinic(Clinic.builder()
				.uuid(UUID.randomUUID().toString())
				.name(signupClinicRequestDto.getClinicName())
				.licenseNumber(signupClinicRequestDto.getLicenseNumber())
				.address(signupClinicRequestDto.getAddress())
				.phone(signupClinicRequestDto.getPhone())
				.timezone(signupClinicRequestDto.getTimezone())
				.email(user.getEmail())
				.owner(user)
				.status(ClinicStatus.PENDING)
				.build());

		zeptoMailService.sendWelcomeEmail(user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	private User createUserWithRole(SignupRequestDto signupRequestDto, ERole roleName) {
		User user = User.builder()
				.uuid(UUID.randomUUID().toString())
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
	public JwtResponseModel loginUser(LoginRequestDto loginRequestDto) {
		
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

		return new JwtResponseModel(jwt, userDetails.getId(), userDetails.getUuid(), userDetails.getEmail(), roles);
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
						.uuid(UUID.randomUUID().toString())
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
				zeptoMailService.sendWelcomeEmail(existingUser.getEmail());
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
