package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteCompleteRequest;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteRequest;
import com.kittyp.clinic.dto.ClinicDtos.StaffMemberModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicStaff;
import com.kittyp.clinic.entity.ClinicStaffInvite;
import com.kittyp.clinic.enums.ClinicStaffInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicStaffInviteRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplStaffInviteTest {

	private static final String CLINIC_UUID = "clinic-staff";

	@Mock
	private UserDao userDao;
	@Mock
	private ClinicDao clinicDao;
	@Mock
	private ClinicStaffDao clinicStaffDao;
	@Mock
	private ClinicDoctorRepository clinicDoctorRepository;
	@Mock
	private ClinicStaffInviteRepository clinicStaffInviteRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private RoleDao roleDao;
	@Mock
	private ZeptoMailService zeptoMailService;

	@InjectMocks
	private ClinicServiceImpl clinicService;

	@Test
	void inviteStaff_existingParentEmail_rejected() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		User parent = userWithRoles(9L, "parent@example.com", ERole.ROLE_USER);
		Clinic clinic = clinic(admin);
		when(userDao.userByEmail("admin@example.com")).thenReturn(admin);
		when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
		when(clinicStaffDao.isActiveMember(clinic.getId(), admin.getId())).thenReturn(false);
		when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), admin.getId()))
				.thenReturn(false);
		when(clinicStaffInviteRepository.countByClinic_IdAndCreatedAtAfter(any(), any())).thenReturn(0L);
		when(userRepository.findByEmailIgnoreCase("parent@example.com")).thenReturn(Optional.of(parent));

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.inviteStaff(CLINIC_UUID,
						new StaffInviteRequest("Pat", "parent@example.com"), "admin@example.com"));
		assertTrue(ex.getMessage().contains("dedicated staff work email"));
		verify(clinicStaffInviteRepository, never()).save(any());
	}

	@Test
	void completeStaffInvite_createsStaffUserAndMembership() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = ClinicStaffInvite.builder()
				.uuid("inv-1")
				.clinic(clinic)
				.email("staff@example.com")
				.staffName("Sam Staff")
				.token("tok")
				.status(ClinicStaffInviteStatus.PENDING)
				.invitedByUserId(admin.getId())
				.expiresAt(LocalDateTime.now().plusDays(2))
				.build();
		when(clinicStaffInviteRepository.findByToken("tok")).thenReturn(Optional.of(invite));
		when(userRepository.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed");
		Role staffRole = new Role();
		staffRole.setName(ERole.ROLE_CLINIC_STAFF);
		when(roleDao.roleByName(ERole.ROLE_CLINIC_STAFF)).thenReturn(staffRole);
		when(userDao.saveUser(any(User.class))).thenAnswer(invocation -> {
			User saved = invocation.getArgument(0);
			saved.setId(40L);
			saved.setUuid("STAF01");
			return saved;
		});
		when(clinicStaffDao.findLatestByClinicAndUser(clinic.getId(), 40L)).thenReturn(Optional.empty());
		when(clinicStaffDao.save(any(ClinicStaff.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clinicStaffInviteRepository.save(any(ClinicStaffInvite.class))).thenAnswer(inv -> inv.getArgument(0));

		StaffMemberModel model = clinicService.completeStaffInvite("tok",
				new StaffInviteCompleteRequest("Sam", "Staff", "Passw0rd!"));

		assertEquals("staff@example.com", model.email());
		assertEquals("Sam Staff", model.name());
		assertEquals("STAF01", model.userUuid());
		assertEquals(ClinicStaffInviteStatus.ACCEPTED, invite.getStatus());
		ArgumentCaptor<ClinicStaff> captor = ArgumentCaptor.forClass(ClinicStaff.class);
		verify(clinicStaffDao).save(captor.capture());
		assertEquals("staff", captor.getValue().getRole());
		assertTrue(Boolean.TRUE.equals(captor.getValue().getIsActive()));
	}

	@Test
	void completeStaffInvite_activeStaffElsewhere_rejected() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		User staff = userWithRoles(8L, "staff@example.com", ERole.ROLE_CLINIC_STAFF);
		staff.setEnabled(false);
		staff.setIsActive(false);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = ClinicStaffInvite.builder()
				.uuid("inv-2")
				.clinic(clinic)
				.email("staff@example.com")
				.staffName("Sam Staff")
				.token("tok2")
				.status(ClinicStaffInviteStatus.PENDING)
				.invitedByUserId(admin.getId())
				.expiresAt(LocalDateTime.now().plusDays(2))
				.build();
		when(clinicStaffInviteRepository.findByToken("tok2")).thenReturn(Optional.of(invite));
		when(userRepository.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));
		when(clinicStaffDao.existsActiveByUserId(8L)).thenReturn(true);
		when(clinicStaffDao.isActiveMember(clinic.getId(), 8L)).thenReturn(false);

		CustomException ex = assertThrows(CustomException.class,
				() -> clinicService.completeStaffInvite("tok2",
						new StaffInviteCompleteRequest("Sam", "Staff", "Passw0rd!")));
		assertTrue(ex.getMessage().contains("already active staff"));
	}

	private static Clinic clinic(User owner) {
		Clinic clinic = Clinic.builder()
				.uuid(CLINIC_UUID)
				.name("Happy Paws")
				.status(ClinicStatus.VERIFIED)
				.owner(owner)
				.build();
		clinic.setId(100L);
		return clinic;
	}

	private static User userWithRoles(Long id, String email, ERole... roles) {
		User user = User.builder().email(email).password("x").uuid("u-" + id)
				.firstName("First").lastName("Last").enabled(true).build();
		user.setId(id);
		user.setIsActive(true);
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
