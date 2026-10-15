package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.kittyp.doctor.dao.DoctorProfileDao;
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
	@Mock
	private DoctorProfileDao doctorProfileDao;

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
		verify(zeptoMailService).sendClinicStaffInviteResponseEmail(
				"admin@example.com", "Happy Paws", "Sam Staff", "staff@example.com", true);
	}

	@Test
	void declineStaffInvite_marksDeclinedAndNotifiesClinic() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-decline");
		when(clinicStaffInviteRepository.findByToken("tok-decline")).thenReturn(Optional.of(invite));
		when(clinicStaffInviteRepository.save(any(ClinicStaffInvite.class))).thenAnswer(inv -> inv.getArgument(0));

		clinicService.declineStaffInvite("tok-decline");

		assertEquals(ClinicStaffInviteStatus.DECLINED, invite.getStatus());
		verify(zeptoMailService).sendClinicStaffInviteResponseEmail(
				"admin@example.com", "Happy Paws", "Sam Staff", "staff@example.com", false);
	}

	@Test
	void declineStaffInvite_expired_doesNotNotify() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-expired");
		invite.setExpiresAt(LocalDateTime.now().minusMinutes(1));
		when(clinicStaffInviteRepository.findByToken("tok-expired")).thenReturn(Optional.of(invite));
		when(clinicStaffInviteRepository.save(any(ClinicStaffInvite.class))).thenAnswer(inv -> inv.getArgument(0));

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.declineStaffInvite("tok-expired"));

		assertTrue(ex.getMessage().contains("expired"));
		assertEquals(ClinicStaffInviteStatus.EXPIRED, invite.getStatus());
		verify(zeptoMailService, never()).sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());
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
		assertEquals(ClinicStaffInviteStatus.PENDING, invite.getStatus());
		verify(zeptoMailService, never()).sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());
	}

	@Test
	void completeStaffInvite_usesClinicEmailWhenPresent() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		clinic.setEmail("  clinic@example.com  ");
		stubNewStaffJoin(clinic, "tok-clinic-mail");

		clinicService.completeStaffInvite("tok-clinic-mail",
				new StaffInviteCompleteRequest("Sam", "Staff", "Passw0rd!"));

		verify(zeptoMailService).sendClinicStaffInviteResponseEmail(
				"clinic@example.com", "Happy Paws", "Sam Staff", "staff@example.com", true);
	}

	@Test
	void completeStaffInvite_expired_doesNotCreateUserOrNotify() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-complete-expired");
		invite.setExpiresAt(LocalDateTime.now().minusMinutes(1));
		when(clinicStaffInviteRepository.findByToken("tok-complete-expired")).thenReturn(Optional.of(invite));
		when(clinicStaffInviteRepository.save(any(ClinicStaffInvite.class))).thenAnswer(inv -> inv.getArgument(0));

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.completeStaffInvite(
				"tok-complete-expired", new StaffInviteCompleteRequest("Sam", "Staff", "Passw0rd!")));

		assertTrue(ex.getMessage().contains("expired"));
		assertEquals(ClinicStaffInviteStatus.EXPIRED, invite.getStatus());
		verify(userDao, never()).saveUser(any());
		verify(zeptoMailService, never()).sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());
	}

	@Test
	void completeStaffInvite_unverifiedClinic_doesNotNotify() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		clinic.setStatus(ClinicStatus.PENDING);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-pending-clinic");
		when(clinicStaffInviteRepository.findByToken("tok-pending-clinic")).thenReturn(Optional.of(invite));

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.completeStaffInvite(
				"tok-pending-clinic", new StaffInviteCompleteRequest("Sam", "Staff", "Passw0rd!")));

		assertTrue(ex.getMessage().toLowerCase().contains("verified"));
		assertEquals(ClinicStaffInviteStatus.PENDING, invite.getStatus());
		verify(zeptoMailService, never()).sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());
	}

	@Test
	void completeStaffInvite_mailFailure_stillJoins() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		stubNewStaffJoin(clinic, "tok-mail-fail");
		doThrow(new RuntimeException("zepto down")).when(zeptoMailService)
				.sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());

		StaffMemberModel model = clinicService.completeStaffInvite("tok-mail-fail",
				new StaffInviteCompleteRequest("Sam", "Staff", "Passw0rd!"));

		assertEquals("STAF01", model.userUuid());
		assertEquals(ClinicStaffInviteStatus.ACCEPTED, clinicStaffInviteRepository.findByToken("tok-mail-fail")
				.orElseThrow().getStatus());
	}

	@Test
	void declineStaffInvite_alreadyAccepted_doesNotNotifyAgain() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-used");
		invite.setStatus(ClinicStaffInviteStatus.ACCEPTED);
		when(clinicStaffInviteRepository.findByToken("tok-used")).thenReturn(Optional.of(invite));

		CustomException ex = assertThrows(CustomException.class, () -> clinicService.declineStaffInvite("tok-used"));

		assertTrue(ex.getMessage().contains("no longer valid"));
		assertEquals(ClinicStaffInviteStatus.ACCEPTED, invite.getStatus());
		verify(zeptoMailService, never()).sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());
	}

	@Test
	void declineStaffInvite_secondCall_doesNotEmailTwice() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-twice");
		when(clinicStaffInviteRepository.findByToken("tok-twice")).thenReturn(Optional.of(invite));
		when(clinicStaffInviteRepository.save(any(ClinicStaffInvite.class))).thenAnswer(inv -> inv.getArgument(0));

		clinicService.declineStaffInvite("tok-twice");
		assertThrows(CustomException.class, () -> clinicService.declineStaffInvite("tok-twice"));

		assertEquals(ClinicStaffInviteStatus.DECLINED, invite.getStatus());
		verify(zeptoMailService, times(1)).sendClinicStaffInviteResponseEmail(
				"admin@example.com", "Happy Paws", "Sam Staff", "staff@example.com", false);
	}

	@Test
	void declineStaffInvite_mailFailure_stillDeclines() {
		User admin = userWithRoles(1L, "admin@example.com", ERole.ROLE_CLINIC_ADMIN);
		Clinic clinic = clinic(admin);
		ClinicStaffInvite invite = pendingInvite(clinic, admin, "tok-decline-mail-fail");
		when(clinicStaffInviteRepository.findByToken("tok-decline-mail-fail")).thenReturn(Optional.of(invite));
		when(clinicStaffInviteRepository.save(any(ClinicStaffInvite.class))).thenAnswer(inv -> inv.getArgument(0));
		doThrow(new RuntimeException("zepto down")).when(zeptoMailService)
				.sendClinicStaffInviteResponseEmail(any(), any(), any(), any(), anyBoolean());

		clinicService.declineStaffInvite("tok-decline-mail-fail");

		assertEquals(ClinicStaffInviteStatus.DECLINED, invite.getStatus());
	}

	private void stubNewStaffJoin(Clinic clinic, String token) {
		ClinicStaffInvite invite = pendingInvite(clinic, clinic.getOwner(), token);
		when(clinicStaffInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
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
	}

	private static ClinicStaffInvite pendingInvite(Clinic clinic, User admin, String token) {
		return ClinicStaffInvite.builder()
				.uuid("inv-" + token)
				.clinic(clinic)
				.email("staff@example.com")
				.staffName("Sam Staff")
				.token(token)
				.status(ClinicStaffInviteStatus.PENDING)
				.invitedByUserId(admin.getId())
				.expiresAt(LocalDateTime.now().plusDays(2))
				.build();
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
