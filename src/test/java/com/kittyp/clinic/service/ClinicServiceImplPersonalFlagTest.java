package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInvitePreview;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteRequest;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplPersonalFlagTest {

	private static final String CLINIC_UUID = "clinic-own";

	@Mock
	private ClinicDao clinicDao;

	@Mock
	private ClinicStaffDao clinicStaffDao;

	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;

	@Mock
	private ClinicDoctorInviteRepository clinicDoctorInviteRepository;

	@Mock
	private UserDao userDao;

	@Mock
	private DoctorProfileDao doctorProfileDao;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void mine_clinicAdminOwnerWithoutDoctorAffiliation_personalFalse() {
		User owner = userWithRoles(1L, "clinic@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(owner);
		stubMineMembership(owner, clinic);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(false);

		List<ClinicModel> mine = clinicService.mine(owner.getEmail());

		assertEquals(1, mine.size());
		assertFalse(Boolean.TRUE.equals(mine.get(0).personal()));
	}

	@Test
	void mine_doctorOwnerWithAffiliation_personalTrue() {
		User owner = userWithRoles(2L, "doc@example.com", ERole.ROLE_DOCTOR);
		Clinic clinic = clinic(owner);
		stubMineMembership(owner, clinic);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), owner.getId()))
				.thenReturn(true);

		List<ClinicModel> mine = clinicService.mine(owner.getEmail());

		assertEquals(1, mine.size());
		assertTrue(Boolean.TRUE.equals(mine.get(0).personal()));
	}

	@Test
	void mine_invitedDoctorOnSoloClinic_personalFalse() {
		User viewer = userWithRoles(3L, "invitee@example.com", ERole.ROLE_DOCTOR);
		User owner = userWithRoles(4L, "owner@example.com", ERole.ROLE_DOCTOR);
		Clinic invited = Clinic.builder()
				.uuid("invited-1")
				.name("Verified Clinic")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		invited.setId(200L);
		when(userDao.userByEmail(viewer.getEmail())).thenReturn(viewer);
		when(clinicDao.findAllByOwnerUserId(viewer.getId())).thenReturn(List.of());
		when(clinicStaffDao.findActiveByUserId(viewer.getId())).thenReturn(List.of());
		when(clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(viewer.getId()))
				.thenReturn(List.of(ClinicDoctor.builder().clinic(invited).build()));

		List<ClinicModel> mine = clinicService.mine(viewer.getEmail());

		assertEquals(1, mine.size());
		assertEquals("invited-1", mine.get(0).uuid());
		assertFalse(Boolean.TRUE.equals(mine.get(0).personal()));
	}

	@Test
	void mine_sortsByName() {
		User owner = userWithRoles(5L, "doc5@example.com", ERole.ROLE_DOCTOR);
		Clinic zeta = Clinic.builder().uuid("z").name("Zeta").status(ClinicStatus.PENDING).owner(owner).build();
		zeta.setId(1L);
		Clinic alpha = Clinic.builder().uuid("a").name("Alpha").status(ClinicStatus.VERIFIED).owner(owner).build();
		alpha.setId(2L);
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);
		when(clinicDao.findAllByOwnerUserId(owner.getId())).thenReturn(List.of(zeta, alpha));
		when(clinicStaffDao.findActiveByUserId(owner.getId())).thenReturn(List.of());
		when(clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(owner.getId())).thenReturn(List.of());
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(1L, 5L)).thenReturn(true);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(2L, 5L)).thenReturn(true);

		List<ClinicModel> mine = clinicService.mine(owner.getEmail());

		assertEquals(List.of("a", "z"), mine.stream().map(ClinicModel::uuid).toList());
	}

	@Test
	void previewInvite_includesClinicUuid() {
		Clinic clinic = Clinic.builder().uuid("clinic-9").name("Alpha").status(ClinicStatus.VERIFIED).build();
		ClinicDoctorInvite invite = ClinicDoctorInvite.builder()
				.clinic(clinic)
				.email("doc@example.com")
				.doctorName("Doc")
				.token("tok")
				.status(ClinicDoctorInviteStatus.PENDING)
				.expiresAt(LocalDateTime.now().plusDays(1))
				.build();
		when(clinicDoctorInviteRepository.findByToken("tok")).thenReturn(Optional.of(invite));

		DoctorInvitePreview preview = clinicService.previewInvite("tok");

		assertEquals("clinic-9", preview.clinicUuid());
		assertEquals("Alpha", preview.clinicName());
	}

	@Test
	void acceptInvite_pendingClinic_rejected() {
		Clinic clinic = Clinic.builder().uuid("c-pending").name("Pending").status(ClinicStatus.PENDING).build();
		clinic.setId(9L);
		ClinicDoctorInvite invite = ClinicDoctorInvite.builder()
				.clinic(clinic)
				.email("doc@example.com")
				.doctorName("Doc")
				.token("tok-pending")
				.status(ClinicDoctorInviteStatus.PENDING)
				.expiresAt(LocalDateTime.now().plusDays(1))
				.build();
		when(clinicDoctorInviteRepository.findByToken("tok-pending")).thenReturn(Optional.of(invite));
		when(userDao.userByEmail("doc@example.com"))
				.thenReturn(userWithRoles(8L, "doc@example.com", ERole.ROLE_DOCTOR));

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.acceptInvite("tok-pending", "doc@example.com"));
		assertEquals(ClinicStatus.NOT_ACTIVATED_MESSAGE, ex.getMessage());
	}

	@Test
	void inviteDoctor_pendingBranch_rejected() {
		User admin = userWithRoles(11L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = Clinic.builder().uuid("branch-p").name("Branch").status(ClinicStatus.PENDING).owner(admin)
				.build();
		clinic.setId(11L);
		when(userDao.userByEmail(admin.getEmail())).thenReturn(admin);
		when(clinicDao.findByUuid("branch-p")).thenReturn(clinic);

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.inviteDoctor("branch-p", new DoctorInviteRequest("Doc", "d@x.com", null),
						admin.getEmail()));
		assertEquals(ClinicStatus.NOT_ACTIVATED_MESSAGE, ex.getMessage());
	}

	@Test
	void inviteDoctor_pendingPersonal_skipsClinicVerify() {
		User owner = userWithRoles(12L, "solo@example.com", ERole.ROLE_CLINIC_ADMIN, ERole.ROLE_DOCTOR);
		Clinic clinic = Clinic.builder().uuid("personal-p").name("NewTrition").status(ClinicStatus.PENDING).owner(owner)
				.build();
		clinic.setId(12L);
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);
		when(clinicDao.findByUuid("personal-p")).thenReturn(clinic);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(12L, 12L)).thenReturn(true);
		when(clinicDoctorInviteRepository.countByClinic_IdAndCreatedAtAfter(eq(12L), any())).thenReturn(0L);

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.inviteDoctor("personal-p", new DoctorInviteRequest(null, null, null),
						owner.getEmail()));
		assertEquals("Provide either doctor email or doctor ID (not both)", ex.getMessage());
	}

	private void stubMineMembership(User owner, Clinic clinic) {
		when(userDao.userByEmail(owner.getEmail())).thenReturn(owner);
		when(clinicDao.findAllByOwnerUserId(owner.getId())).thenReturn(List.of(clinic));
		when(clinicStaffDao.findActiveByUserId(owner.getId())).thenReturn(List.of());
		when(clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(owner.getId())).thenReturn(List.of());
	}

	private static Clinic clinic(User owner) {
		Clinic clinic = Clinic.builder()
				.uuid(CLINIC_UUID)
				.name("Branch")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		clinic.setId(100L);
		return clinic;
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
