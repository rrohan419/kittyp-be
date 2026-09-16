package com.kittyp.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneAvailabilityService {

	public static final String ALREADY_IN_USE = "Phone number is already in use";

	private final UserRepository userRepository;
	private final ClinicPetOwnerRepository clinicPetOwnerRepository;
	private final DoctorProfileRepository doctorProfileRepository;
	private final ClinicRepository clinicRepository;

	/** Blocks if last 10 digits belong to any other user, clinic, clinic client, or doctor profile. */
	public void assertAvailable(String local10, User currentUser) {
		if (local10 == null || !local10.matches("\\d{10}")) {
			return;
		}
		if (ClinicOwnerUserLinkService.PLACEHOLDER_PHONE.equals(local10)) {
			return;
		}
		String exceptUuid = currentUser == null ? null : currentUser.getUuid();
		Long exceptUserId = currentUser == null ? null : currentUser.getId();
		if (userRepository.countByLocal10DigitsExcludingUuid(local10, exceptUuid) > 0
				|| clinicPetOwnerRepository.countActiveByLocal10ExcludingLinkedUser(local10, exceptUserId) > 0
				|| doctorProfileRepository.countByLocal10ExcludingUserUuid(local10, exceptUuid) > 0
				|| clinicRepository.countByLocal10ExcludingOwnerUserId(local10, exceptUserId) > 0) {
			throw new CustomException(ALREADY_IN_USE, HttpStatus.CONFLICT);
		}
	}
}
