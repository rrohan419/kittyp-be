package com.kittyp.auth.tenant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAccessService {

	private static final String AUDIT_LOGGER = "SECURITY_AUDIT";
	private static final org.slf4j.Logger AUDIT = org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER);

	private final ClinicDao clinicDao;
	private final ClinicStaffDao clinicStaffDao;
	private final ClinicDoctorRepository clinicDoctorRepository;
	private final UserDao userDao;

	public Clinic requireClinic(String clinicUuid) {
		Clinic clinic = clinicDao.findByUuid(clinicUuid);
		if (clinic == null) {
			throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
		}
		return clinic;
	}

	public boolean isMember(Clinic clinic, User user) {
		if (clinic == null || clinic.getId() == null || user == null || user.getId() == null) {
			return false;
		}
		Long ownerId = clinicDao.findOwnerUserId(clinic.getId());
		if (ownerId != null && ownerId.equals(user.getId())) {
			return true;
		}
		if (clinicStaffDao.isActiveMember(clinic.getId(), user.getId())) {
			return true;
		}
		return clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), user.getId());
	}

	public void requireMember(String clinicUuid, String email, String path) {
		Clinic clinic = requireClinic(clinicUuid);
		requireMember(clinic, email, path);
	}

	public void requireMember(Clinic clinic, String email, String path) {
		User user = userDao.userByEmail(email);
		if (isMember(clinic, user)) {
			return;
		}
		deny(email, clinic.getUuid(), path);
	}

	public void deny(String actor, String clinicUuid, String path) {
		AUDIT.warn("event=TENANT_ISOLATION_DENIED actor={} clinicUuid={} path={}", actor, clinicUuid, path);
		throw new AccessDeniedException("You do not have access to this clinic.");
	}
}
