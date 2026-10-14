package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ClinicOwnerUserLinkServicePhoneSyncTest {

	@Mock
	private ClinicPetOwnerRepository clinicPetOwnerRepository;
	@Mock
	private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private UserDao userDao;
	private ClinicOwnerUserLinkService service;

	@BeforeEach
	void setUp() {
		service = new ClinicOwnerUserLinkService(
				clinicPetOwnerRepository, clinicPetEnrollmentRepository, userRepository, userDao);
	}

	@Test
	void syncOwnerPhoneFromUser_replacesPlaceholder() {
		User user = user(1L, "parent@example.com", "9876543210");
		ClinicPetOwner owner = owner(10L, "parent@example.com", KeyConstant.PLACEHOLDER_PHONE, user);

		when(clinicPetOwnerRepository.save(any(ClinicPetOwner.class))).thenAnswer(inv -> inv.getArgument(0));

		ClinicPetOwner updated = service.syncOwnerPhoneFromUser(owner, user);

		assertEquals("9876543210", updated.getPhone());
		ArgumentCaptor<ClinicPetOwner> captor = ArgumentCaptor.forClass(ClinicPetOwner.class);
		verify(clinicPetOwnerRepository).save(captor.capture());
		assertEquals("9876543210", captor.getValue().getPhone());
	}

	@Test
	void syncOwnerPhoneFromUser_noOpWhenAlreadyMatches() {
		User user = user(1L, "parent@example.com", "9876543210");
		ClinicPetOwner owner = owner(10L, "parent@example.com", "9876543210", user);

		ClinicPetOwner updated = service.syncOwnerPhoneFromUser(owner, user);

		assertEquals("9876543210", updated.getPhone());
		verify(clinicPetOwnerRepository, never()).save(any());
	}

	@Test
	void syncOwnerPhoneFromUser_noOpWhenUserPhoneInvalid() {
		User user = user(1L, "parent@example.com", "123");
		ClinicPetOwner owner = owner(10L, "parent@example.com", KeyConstant.PLACEHOLDER_PHONE, user);

		ClinicPetOwner updated = service.syncOwnerPhoneFromUser(owner, user);

		assertEquals(KeyConstant.PLACEHOLDER_PHONE, updated.getPhone());
		verify(clinicPetOwnerRepository, never()).save(any());
	}

	@Test
	void linkUserToClinicOwners_syncsPhoneOnAlreadyLinkedOwners() {
		User user = user(1L, "parent@example.com", "9123456789");
		ClinicPetOwner owner = owner(10L, "parent@example.com", KeyConstant.PLACEHOLDER_PHONE, user);

		when(clinicPetOwnerRepository.findByIsActiveTrueAndEmailIgnoreCase("parent@example.com"))
				.thenReturn(List.of(owner));
		when(clinicPetOwnerRepository.findByLinkedUser_IdAndIsActiveTrue(1L)).thenReturn(List.of(owner));
		when(userDao.userByUuid("user-uuid")).thenReturn(user);
		when(clinicPetOwnerRepository.save(any(ClinicPetOwner.class))).thenAnswer(inv -> inv.getArgument(0));
		service.linkUserToClinicOwners(user);

		assertEquals("9123456789", owner.getPhone());
		verify(clinicPetOwnerRepository).save(owner);
	}

	@Test
	void linkOwnerAttachesPetFromClinicEnrollment() {
		User user = user(1L, "parent@example.com", "9123456789");
		ClinicPetOwner owner = owner(10L, "parent@example.com", "9123456789", user);
		Pet pet = Pet.builder().uuid("pet-uuid").build();
		pet.setId(30L);
		ClinicPetEnrollment enrollment = ClinicPetEnrollment.builder()
				.clinic(owner.getClinic())
				.clinicOwner(owner)
				.pet(pet)
				.build();
		User managed = user(1L, "parent@example.com", "9123456789");

		when(userDao.userByUuid("user-uuid")).thenReturn(managed);
		when(clinicPetEnrollmentRepository.findByClinic_IdAndIsActiveTrue(20L))
				.thenReturn(List.of(enrollment));

		service.linkOwnerIfUserExists(owner);

		assertEquals(1, managed.getPets().size());
		assertEquals(pet, managed.getPets().get(0));
	}

	private static User user(Long id, String email, String phone) {
		User user = new User();
		user.setId(id);
		user.setUuid("user-uuid");
		user.setEmail(email);
		user.setPhoneNumber(phone);
		return user;
	}

	private static ClinicPetOwner owner(Long id, String email, String phone, User linked) {
		ClinicPetOwner owner = new ClinicPetOwner();
		owner.setId(id);
		owner.setUuid("owner-uuid");
		owner.setEmail(email);
		Clinic clinic = Clinic.builder().build();
		clinic.setId(20L);
		owner.setClinic(clinic);
		owner.setPhone(phone);
		owner.setFirstName("Ada");
		owner.setLinkedUser(linked);
		owner.setIsActive(true);
		return owner;
	}
}
