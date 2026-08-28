package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.ClinicDoctorDetailModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.visit.dao.VisitDao;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplDoctorCertAuthTest {

	private static final String CLINIC_UUID = "clinic-b";
	private static final String TARGET_DOCTOR_UUID = "doc-target";
	private static final String DEGREE_URL = "https://files.example/degree.pdf";
	private static final String REG_URL = "https://files.example/reg.pdf";
	private static final String GOV_URL = "https://files.example/gov.pdf";
	private static final String LICENSE_URL = "https://files.example/license.pdf";
	private static final String PHOTOS_URL = "https://files.example/clinic.jpg";

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicStaffDao clinicStaffDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private UserDao userDao;

	@Mock
	private VisitDao visitDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void doctorDetail_peerDoctor_redactsCertificateUrls() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User viewer = userWithRoles(10L, "doc-a@example.com", ERole.ROLE_DOCTOR);
		User targetUser = userWithRoles(20L, "doc-b@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		DoctorProfile target = doctorProfile(targetUser);
		stubClinicAccess(clinic, viewer, false, true);
		stubDoctorAffiliation(clinic, target);

		ClinicDoctorDetailModel detail = clinicService.doctorDetail(CLINIC_UUID, TARGET_DOCTOR_UUID, viewer.getEmail());

		assertEquals(TARGET_DOCTOR_UUID, detail.doctorUuid());
		assertEquals("First Last", detail.name());
		assertEquals("doc-b@example.com", detail.email());
		assertEquals("VET-99", detail.registrationNumber());
		assertNull(detail.licenseNumber());
		assertNull(detail.status());
		assertNull(detail.reviewNotes());
		assertNull(detail.degreeCertificateUrl());
		assertNull(detail.registrationCertificateUrl());
		assertNull(detail.governmentIdUrl());
		assertNull(detail.licenseDocumentUrl());
		assertNull(detail.clinicPhotosUrls());
		assertEquals(List.of(), detail.patients());
	}

	@Test
	void doctorDetail_self_includesCertificateUrls() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User targetUser = userWithRoles(20L, "doc-b@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		DoctorProfile target = doctorProfile(targetUser);
		stubClinicAccess(clinic, targetUser, false, true);
		stubDoctorDetailWithPatients(clinic, target);

		ClinicDoctorDetailModel detail = clinicService.doctorDetail(CLINIC_UUID, TARGET_DOCTOR_UUID,
				targetUser.getEmail());

		assertCertificateUrlsPresent(detail);
	}

	@Test
	void doctorDetail_clinicAdminOwner_includesCertificateUrls() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User targetUser = userWithRoles(20L, "doc-b@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		DoctorProfile target = doctorProfile(targetUser);
		stubClinicAccess(clinic, owner, false, false);
		stubDoctorDetailWithPatients(clinic, target);

		ClinicDoctorDetailModel detail = clinicService.doctorDetail(CLINIC_UUID, TARGET_DOCTOR_UUID, owner.getEmail());

		assertCertificateUrlsPresent(detail);
	}

	@Test
	void doctorDetail_clinicAdminStaff_includesCertificateUrls() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User staffAdmin = userWithRoles(5L, "admin-staff@example.com", ERole.ROLE_CLINIC_ADMIN);
		User targetUser = userWithRoles(20L, "doc-b@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		DoctorProfile target = doctorProfile(targetUser);
		stubClinicAccess(clinic, staffAdmin, true, false);
		stubDoctorDetailWithPatients(clinic, target);

		ClinicDoctorDetailModel detail = clinicService.doctorDetail(CLINIC_UUID, TARGET_DOCTOR_UUID,
				staffAdmin.getEmail());

		assertCertificateUrlsPresent(detail);
	}

	@Test
	void doctorDetail_clinicStaffOnly_redactsCertificateUrls() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User staff = userWithRoles(6L, "staff@example.com", ERole.ROLE_CLINIC_STAFF);
		User targetUser = userWithRoles(20L, "doc-b@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		DoctorProfile target = doctorProfile(targetUser);
		stubClinicAccess(clinic, staff, true, false);
		stubDoctorAffiliation(clinic, target);

		ClinicDoctorDetailModel detail = clinicService.doctorDetail(CLINIC_UUID, TARGET_DOCTOR_UUID, staff.getEmail());

		assertNull(detail.degreeCertificateUrl());
		assertNull(detail.registrationCertificateUrl());
		assertNull(detail.governmentIdUrl());
		assertNull(detail.licenseDocumentUrl());
		assertNull(detail.clinicPhotosUrls());
	}

	@Test
	void doctorDetail_crossTenantClinicAdminAffiliatedAsDoctor_redactsCertificateUrls() {
		User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
		User otherClinicAdmin = userWithRoles(7L, "admin-a@example.com", ERole.ROLE_CLINIC_ADMIN);
		User targetUser = userWithRoles(20L, "doc-b@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		DoctorProfile target = doctorProfile(targetUser);
		stubClinicAccess(clinic, otherClinicAdmin, false, true);
		stubDoctorAffiliation(clinic, target);

		ClinicDoctorDetailModel detail = clinicService.doctorDetail(CLINIC_UUID, TARGET_DOCTOR_UUID,
				otherClinicAdmin.getEmail());

		assertNull(detail.degreeCertificateUrl());
		assertNull(detail.registrationCertificateUrl());
		assertNull(detail.governmentIdUrl());
		assertNull(detail.licenseDocumentUrl());
		assertNull(detail.clinicPhotosUrls());
	}

	private void stubClinicAccess(Clinic clinic, User viewer, boolean staffHere, boolean doctorHere) {
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(userDao.userByEmail(viewer.getEmail())).thenReturn(viewer);
		when(clinicStaffDao.isActiveMember(clinic.getId(), viewer.getId())).thenReturn(staffHere);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), viewer.getId()))
				.thenReturn(doctorHere);
	}

	private void stubDoctorAffiliation(Clinic clinic, DoctorProfile target) {
		ClinicDoctor affiliation = ClinicDoctor.builder()
				.clinic(clinic)
				.doctor(target)
				.role("doctor")
				.isActive(true)
				.build();
		when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(clinic.getId(), TARGET_DOCTOR_UUID))
				.thenReturn(Optional.of(affiliation));
	}

	private void stubDoctorDetailWithPatients(Clinic clinic, DoctorProfile target) {
		stubDoctorAffiliation(clinic, target);
		when(visitDao.findByClinicAndDoctor(clinic.getId(), target.getId())).thenReturn(List.of());
	}

	private static void assertCertificateUrlsPresent(ClinicDoctorDetailModel detail) {
		assertEquals(DEGREE_URL, detail.degreeCertificateUrl());
		assertEquals(REG_URL, detail.registrationCertificateUrl());
		assertEquals(GOV_URL, detail.governmentIdUrl());
		assertEquals(LICENSE_URL, detail.licenseDocumentUrl());
		assertEquals(PHOTOS_URL, detail.clinicPhotosUrls());
		assertEquals("LIC-1", detail.licenseNumber());
		assertEquals("VET-99", detail.registrationNumber());
		assertEquals("internal review", detail.reviewNotes());
	}

	private static Clinic clinic(User owner) {
		Clinic clinic = Clinic.builder()
				.uuid(CLINIC_UUID)
				.name("Clinic B")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		clinic.setId(100L);
		return clinic;
	}

	private static DoctorProfile doctorProfile(User user) {
		DoctorProfile profile = DoctorProfile.builder()
				.uuid(TARGET_DOCTOR_UUID)
				.user(user)
				.status(DoctorStatus.DOCUMENTS_SUBMITTED)
				.registrationNumber("VET-99")
				.licenseNumber("LIC-1")
				.reviewNotes("internal review")
				.checkDegree(true)
				.degreeCertificateUrl(DEGREE_URL)
				.registrationCertificateUrl(REG_URL)
				.governmentIdUrl(GOV_URL)
				.licenseDocumentUrl(LICENSE_URL)
				.clinicPhotosUrls(PHOTOS_URL)
				.build();
		profile.setId(200L);
		return profile;
	}

	private static User userWithRoles(Long id, String email, ERole... roles) {
		User user = User.builder().email(email).password("x").uuid("u-" + id)
				.firstName("First").lastName("Last").build();
		user.setId(id);
		Set<UserRole> userRoles = new HashSet<>();
		for (ERole eRole : roles) {
			Role role = new Role();
			role.setName(eRole);
			userRoles.add(new UserRole(user, role));
		}
		user.setUserRoles(userRoles);
		return user;
	}
}
