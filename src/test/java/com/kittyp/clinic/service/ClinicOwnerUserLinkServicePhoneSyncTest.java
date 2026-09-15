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
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ClinicOwnerUserLinkServicePhoneSyncTest {

	@Mock
	private ClinicPetOwnerRepository clinicPetOwnerRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private UserDao userDao;
	@Mock
	private PetsRepository petsRepository;

	private ClinicOwnerUserLinkService service;

	@BeforeEach
	void setUp() {
		service = new ClinicOwnerUserLinkService(
				clinicPetOwnerRepository, userRepository, userDao, petsRepository);
	}

	@Test
	void syncOwnerPhoneFromUser_replacesPlaceholder() {
		User user = user(1L, "parent@example.com", "9876543210");
		ClinicPetOwner owner = owner(10L, "parent@example.com", ClinicOwnerUserLinkService.PLACEHOLDER_PHONE, user);

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
		ClinicPetOwner owner = owner(10L, "parent@example.com", ClinicOwnerUserLinkService.PLACEHOLDER_PHONE, user);

		ClinicPetOwner updated = service.syncOwnerPhoneFromUser(owner, user);

		assertEquals(ClinicOwnerUserLinkService.PLACEHOLDER_PHONE, updated.getPhone());
		verify(clinicPetOwnerRepository, never()).save(any());
	}

	@Test
	void linkUserToClinicOwners_syncsPhoneOnAlreadyLinkedOwners() {
		User user = user(1L, "parent@example.com", "9123456789");
		ClinicPetOwner owner = owner(10L, "parent@example.com", ClinicOwnerUserLinkService.PLACEHOLDER_PHONE, user);

		when(clinicPetOwnerRepository.findByIsActiveTrueAndEmailIgnoreCase("parent@example.com"))
				.thenReturn(List.of(owner));
		when(clinicPetOwnerRepository.findByLinkedUser_IdAndIsActiveTrue(1L)).thenReturn(List.of(owner));
		when(userDao.userByUuid("user-uuid")).thenReturn(user);
		when(clinicPetOwnerRepository.save(any(ClinicPetOwner.class))).thenAnswer(inv -> inv.getArgument(0));
		when(petsRepository.findByClinicOwner_IdAndIsActiveTrue(10L)).thenReturn(List.of());

		service.linkUserToClinicOwners(user);

		assertEquals("9123456789", owner.getPhone());
		verify(clinicPetOwnerRepository).save(owner);
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
		owner.setPhone(phone);
		owner.setFirstName("Ada");
		owner.setLinkedUser(linked);
		owner.setIsActive(true);
		return owner;
	}
}
