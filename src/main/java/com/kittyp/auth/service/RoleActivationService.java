package com.kittyp.auth.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.auth.dto.ActivateRoleRequest;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

/**
 * Adds a missing self-service role to the authenticated user. Role, profile,
 * and clinic rows commit together. A failure rolls the whole unit back.
 */
@Service
public class RoleActivationService {

	static final String PARENT_ADDED = "Pet parent role added.";
	static final String DOCTOR_ADDED = "Doctor role added. Verification is pending.";
	static final String CLINIC_ADDED = "Clinic admin role added. Clinic verification is pending.";

	private final UserDao userDao;
	private final RoleDao roleDao;
	private final DoctorProfileDao doctorProfileDao;
	private final ClinicDao clinicDao;
	private final VerificationCodeService verificationCodeService;
	private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
	private final AuthServiceImpl authService;
	private final RoleCredentialService roleCredentialService;

	public RoleActivationService(UserDao userDao, RoleDao roleDao, DoctorProfileDao doctorProfileDao,
			ClinicDao clinicDao, VerificationCodeService verificationCodeService,
			ClinicOwnerUserLinkService clinicOwnerUserLinkService, @Lazy AuthServiceImpl authService,
			RoleCredentialService roleCredentialService) {
		this.userDao = userDao;
		this.roleDao = roleDao;
		this.doctorProfileDao = doctorProfileDao;
		this.clinicDao = clinicDao;
		this.verificationCodeService = verificationCodeService;
		this.clinicOwnerUserLinkService = clinicOwnerUserLinkService;
		this.authService = authService;
		this.roleCredentialService = roleCredentialService;
	}

	public static void assertSelfService(ERole role) {
		if (role == null || role == ERole.ROLE_ADMIN || role == ERole.ROLE_MODERATOR
				|| role == ERole.ROLE_CLINIC_STAFF) {
			throw new CustomException("This role cannot be self-assigned", HttpStatus.FORBIDDEN);
		}
	}

	@Transactional
	public RoleActivationResult activate(String sessionEmail, ActivateRoleRequest request) {
		if (sessionEmail == null || sessionEmail.isBlank()) {
			throw new CustomException("Authentication required", HttpStatus.UNAUTHORIZED);
		}
		if (request == null || request.getRole() == null) {
			throw new CustomException("Role is required", HttpStatus.BAD_REQUEST);
		}
		assertSelfService(request.getRole().toERole());
		if (request.getEmail() != null && !request.getEmail().isBlank()
				&& !request.getEmail().trim().equalsIgnoreCase(sessionEmail.trim())) {
			throw new CustomException("Email does not match the signed-in account", HttpStatus.FORBIDDEN);
		}

		User loaded = userDao.userByEmail(sessionEmail.trim());
		User user = userDao.lockById(loaded.getId());
		return switch (request.getRole()) {
			case USER -> activateParent(user, request);
			case DOCTOR -> activateDoctor(user, request);
			case CLINIC -> activateClinic(user, request);
		};
	}

	private RoleActivationResult activateParent(User user, ActivateRoleRequest request) {
		if (hasRole(user, ERole.ROLE_USER)) {
			return RoleActivationResult.alreadyActive();
		}
		addRole(user, ERole.ROLE_USER);
		userDao.saveUser(user);
		clinicOwnerUserLinkService.linkUserToClinicOwners(user);
		return created(user, ERole.ROLE_USER, request, PARENT_ADDED, user.getFirstName(),
				RoleActivationResult.WelcomeEmail.PARENT);
	}

	private RoleActivationResult activateDoctor(User user, ActivateRoleRequest request) {
		DoctorProfile existing = doctorProfileDao.findByUserId(user.getId());
		if (existing != null) {
			if (hasRole(user, ERole.ROLE_DOCTOR)) {
				return RoleActivationResult.alreadyActive();
			}
			addRole(user, ERole.ROLE_DOCTOR);
			userDao.saveUser(user);
			return created(user, ERole.ROLE_DOCTOR, request, DOCTOR_ADDED, user.getFirstName(),
					RoleActivationResult.WelcomeEmail.NONE);
		}

		SignupDoctorRequestDto doctor = toDoctorRequest(user, request);
		SignupRequirements.requireDoctor(doctor, verificationCodeService, false);
		if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
			user.setPhoneNumber(doctor.getPhoneNumber());
			user.setPhoneCountryCode("+91");
			user = userDao.saveUser(user);
		}
		authService.provisionNewDoctor(user, doctor);
		if (!hasRole(user, ERole.ROLE_DOCTOR)) {
			addRole(user, ERole.ROLE_DOCTOR);
			userDao.saveUser(user);
		}
		return created(user, ERole.ROLE_DOCTOR, request, DOCTOR_ADDED, user.getFirstName(),
				RoleActivationResult.WelcomeEmail.DOCTOR);
	}

	private RoleActivationResult activateClinic(User user, ActivateRoleRequest request) {
		if (hasRole(user, ERole.ROLE_CLINIC_ADMIN)) {
			return RoleActivationResult.alreadyActive();
		}
		SignupClinicRequestDto clinicRequest = toClinicRequest(user, request);
		SignupRequirements.requireClinic(clinicRequest, verificationCodeService, false);
		clinicDao.saveClinic(Clinic.builder()
				.name(clinicRequest.getClinicName())
				.licenseNumber(clinicRequest.getLicenseNumber())
				.address(clinicRequest.getAddress())
				.phone(clinicRequest.getPhone())
				.timezone(clinicRequest.getTimezone())
				.email(user.getEmail())
				.owner(user)
				.status(ClinicStatus.PENDING)
				.build());
		addRole(user, ERole.ROLE_CLINIC_ADMIN);
		userDao.saveUser(user);
		return created(user, ERole.ROLE_CLINIC_ADMIN, request, CLINIC_ADDED, user.getFirstName(),
				RoleActivationResult.WelcomeEmail.CLINIC);
	}

	private RoleActivationResult created(User user, ERole role, ActivateRoleRequest request, String message,
			String firstName, RoleActivationResult.WelcomeEmail welcome) {
		roleCredentialService.storeNewRolePassword(user, role, request == null ? null : request.getRolePassword());
		return new RoleActivationResult(message, firstName, welcome);
	}

	private void addRole(User user, ERole roleName) {
		Role role = roleDao.roleByName(roleName);
		if (role == null) {
			throw new CustomException("Role " + roleName + " is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		user.addRole(role);
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

	private static SignupDoctorRequestDto toDoctorRequest(User user, ActivateRoleRequest request) {
		SignupDoctorRequestDto dto = new SignupDoctorRequestDto();
		dto.setEmail(user.getEmail());
		dto.setFirstName(user.getFirstName());
		dto.setLastName(user.getLastName());
		dto.setPhoneNumber(request.getPhoneNumber());
		dto.setLicenseNumber(request.getLicenseNumber());
		dto.setRegistrationNumber(request.getRegistrationNumber());
		dto.setSpecialization(request.getSpecialization());
		dto.setExperience(request.getExperience());
		dto.setProfessionalSummary(request.getProfessionalSummary());
		dto.setDegreeCertificateUrl(request.getDegreeCertificateUrl());
		dto.setRegistrationCertificateUrl(request.getRegistrationCertificateUrl());
		dto.setGovernmentIdUrl(request.getGovernmentIdUrl());
		dto.setPhotoUrl(request.getPhotoUrl());
		dto.setInviteToken(request.getInviteToken());
		return dto;
	}

	private static SignupClinicRequestDto toClinicRequest(User user, ActivateRoleRequest request) {
		SignupClinicRequestDto dto = new SignupClinicRequestDto();
		dto.setEmail(user.getEmail());
		dto.setFirstName(user.getFirstName());
		dto.setLastName(user.getLastName());
		dto.setClinicName(request.getClinicName());
		dto.setLicenseNumber(request.getLicenseNumber());
		dto.setAddress(request.getAddress());
		dto.setPhone(request.getPhone());
		dto.setTimezone(request.getTimezone());
		return dto;
	}
}
