package com.kittyp.clinic.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Links clinic pet owners to platform users by shared email or phone,
 * and attaches existing {@code pets} rows to the user (no duplicate pets).
 */
@Service
@RequiredArgsConstructor
public class ClinicOwnerUserLinkService {

	private static final Logger log = LoggerFactory.getLogger(ClinicOwnerUserLinkService.class);

	private final ClinicPetOwnerRepository clinicPetOwnerRepository;
	private final UserRepository userRepository;
	private final UserDao userDao;
	private final PetsRepository petsRepository;

	public static String normalizePhoneDigits(String phone) {
		if (phone == null) {
			return null;
		}
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() >= 10) {
			return digits.substring(digits.length() - 10);
		}
		return digits.isEmpty() ? null : digits;
	}

	public static String normalizeEmail(String email) {
		return email == null || email.isBlank() ? null : email.trim().toLowerCase();
	}

	@Transactional
	public ClinicPetOwner linkOwnerIfUserExists(ClinicPetOwner owner) {
		if (owner == null) {
			return owner;
		}
		if (owner.getLinkedUser() != null) {
			// Already linked — do not silently re-attach pets from clinic-side writes.
			return owner;
		}
		String email = normalizeEmail(owner.getEmail());
		String phone = normalizePhoneDigits(owner.getPhone());

		User matched = null;
		if (email != null) {
			matched = userRepository.findByEmail(email).orElse(null);
		}
		if (matched == null && phone != null && phone.matches("\\d{10}")) {
			List<User> byPhone = userRepository.findByPhoneDigits(phone);
			if (byPhone.size() == 1) {
				matched = byPhone.get(0);
			} else if (byPhone.size() > 1) {
				log.warn("Ambiguous phone match for clinic owner {} — skipping link", owner.getUuid());
				return owner;
			}
		}
		if (matched != null) {
			// Soft-link for clinic billing/UI only. Pets attach when the platform user
			// signs up / updates profile (linkUserToClinicOwners) — never from clinic staff writes.
			owner.setLinkedUser(matched);
			owner = clinicPetOwnerRepository.save(owner);
			log.info("Soft-linked clinic owner {} to user {} (pets not auto-attached)", owner.getUuid(),
					matched.getUuid());
		}
		return owner;
	}

	@Transactional
	public int linkUserToClinicOwners(User user) {
		if (user == null) {
			return 0;
		}
		String email = normalizeEmail(user.getEmail());
		String phone = normalizePhoneDigits(user.getPhoneNumber());

		List<ClinicPetOwner> byEmail = email == null
				? List.of()
				: clinicPetOwnerRepository.findByLinkedUserIsNullAndIsActiveTrueAndEmailIgnoreCase(email);
		List<ClinicPetOwner> byPhone = phone == null || !phone.matches("\\d{10}")
				? List.of()
				: clinicPetOwnerRepository.findByLinkedUserIsNullAndIsActiveTrueAndPhone(phone);

		Set<Long> emailIds = byEmail.stream().map(ClinicPetOwner::getId).collect(Collectors.toSet());
		Set<Long> phoneIds = byPhone.stream().map(ClinicPetOwner::getId).collect(Collectors.toSet());

		Set<String> emails = Stream.concat(byEmail.stream(), byPhone.stream())
				.map(o -> normalizeEmail(o.getEmail()))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<String> phones = Stream.concat(byEmail.stream(), byPhone.stream())
				.map(o -> normalizePhoneDigits(o.getPhone()))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (!emailIds.isEmpty() && !phoneIds.isEmpty()) {
			Set<Long> intersection = new HashSet<>(emailIds);
			intersection.retainAll(phoneIds);
			if (intersection.isEmpty() && emails.size() > 1 && phones.size() > 1) {
				log.warn("Ambiguous clinic-owner match for user {} (email vs phone conflict) — skipping auto-link",
						user.getUuid());
				return 0;
			}
		}

		Set<Long> toLink = new HashSet<>();
		toLink.addAll(emailIds);
		toLink.addAll(phoneIds);
		if (toLink.isEmpty()) {
			return 0;
		}

		int linked = 0;
		for (ClinicPetOwner owner : Stream.concat(byEmail.stream(), byPhone.stream())
				.filter(o -> toLink.contains(o.getId()))
				.collect(Collectors.toMap(ClinicPetOwner::getId, o -> o, (a, b) -> a))
				.values()) {
			if (owner.getLinkedUser() != null) {
				continue;
			}
			owner.setLinkedUser(user);
			clinicPetOwnerRepository.save(owner);
			attachPetsToUser(owner, user);
			linked++;
		}
		if (linked > 0) {
			log.info("Linked {} clinic owner(s) to user {}", linked, user.getUuid());
		}
		return linked;
	}

	/** Attach clinic-registered pets (already in {@code pets}) to the platform user. */
	private void attachPetsToUser(ClinicPetOwner owner, User user) {
		List<Pet> clinicPets = petsRepository.findByClinicOwner_IdAndIsActiveTrue(owner.getId());
		User managed = userDao.userByUuid(user.getUuid());
		Set<String> owned = managed.getPets() == null ? Set.of()
				: managed.getPets().stream().map(Pet::getUuid).collect(Collectors.toSet());
		boolean changed = false;
		for (Pet pet : clinicPets) {
			if (owned.contains(pet.getUuid())) {
				continue;
			}
			managed.addPet(pet);
			changed = true;
		}
		if (changed) {
			userDao.saveUser(managed);
		}
	}
}
