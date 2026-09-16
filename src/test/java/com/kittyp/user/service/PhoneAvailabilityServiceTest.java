package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PhoneAvailabilityServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private ClinicPetOwnerRepository clinicPetOwnerRepository;
	@Mock
	private DoctorProfileRepository doctorProfileRepository;
	@Mock
	private ClinicRepository clinicRepository;

	private PhoneAvailabilityService service;

	@BeforeEach
	void setUp() {
		service = new PhoneAvailabilityService(userRepository, clinicPetOwnerRepository, doctorProfileRepository,
				clinicRepository);
	}

	@Test
	void assertAvailable_userTaken_throwsConflict() {
		when(userRepository.countByLocal10DigitsExcludingUuid("7798296970", "me")).thenReturn(1L);
		User current = User.builder().uuid("me").email("me@kittyp.test").password("x").build();
		current.setId(1L);

		CustomException ex = assertThrows(CustomException.class,
				() -> service.assertAvailable("7798296970", current));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		assertEquals(PhoneAvailabilityService.ALREADY_IN_USE, ex.getMessage());
	}

	@Test
	void assertAvailable_clinicTaken_throwsConflict() {
		when(userRepository.countByLocal10DigitsExcludingUuid("7798296970", null)).thenReturn(0L);
		when(clinicPetOwnerRepository.countActiveByLocal10ExcludingLinkedUser("7798296970", null)).thenReturn(1L);

		CustomException ex = assertThrows(CustomException.class, () -> service.assertAvailable("7798296970", null));
		assertEquals(PhoneAvailabilityService.ALREADY_IN_USE, ex.getMessage());
	}

	@Test
	void assertAvailable_doctorTaken_throwsConflict() {
		when(userRepository.countByLocal10DigitsExcludingUuid("7798296970", null)).thenReturn(0L);
		when(clinicPetOwnerRepository.countActiveByLocal10ExcludingLinkedUser("7798296970", null)).thenReturn(0L);
		when(doctorProfileRepository.countByLocal10ExcludingUserUuid("7798296970", null)).thenReturn(1L);

		assertThrows(CustomException.class, () -> service.assertAvailable("7798296970", null));
	}

	@Test
	void assertAvailable_clinicRowTaken_throwsConflict() {
		when(userRepository.countByLocal10DigitsExcludingUuid("9384720938", null)).thenReturn(0L);
		when(clinicPetOwnerRepository.countActiveByLocal10ExcludingLinkedUser("9384720938", null)).thenReturn(0L);
		when(doctorProfileRepository.countByLocal10ExcludingUserUuid("9384720938", null)).thenReturn(0L);
		when(clinicRepository.countByLocal10ExcludingOwnerUserId("9384720938", null)).thenReturn(1L);

		CustomException ex = assertThrows(CustomException.class, () -> service.assertAvailable("9384720938", null));
		assertEquals(PhoneAvailabilityService.ALREADY_IN_USE, ex.getMessage());
	}

	@Test
	void assertAvailable_free_doesNotThrow() {
		when(userRepository.countByLocal10DigitsExcludingUuid("7798296970", null)).thenReturn(0L);
		when(clinicPetOwnerRepository.countActiveByLocal10ExcludingLinkedUser("7798296970", null)).thenReturn(0L);
		when(doctorProfileRepository.countByLocal10ExcludingUserUuid("7798296970", null)).thenReturn(0L);
		when(clinicRepository.countByLocal10ExcludingOwnerUserId("7798296970", null)).thenReturn(0L);

		service.assertAvailable("7798296970", null);

		verify(userRepository).countByLocal10DigitsExcludingUuid("7798296970", null);
	}

	@Test
	void assertAvailable_placeholder_skipsLookup() {
		service.assertAvailable(ClinicOwnerUserLinkService.PLACEHOLDER_PHONE, null);
		verifyNoInteractions(userRepository, clinicPetOwnerRepository, doctorProfileRepository, clinicRepository);
	}
}
