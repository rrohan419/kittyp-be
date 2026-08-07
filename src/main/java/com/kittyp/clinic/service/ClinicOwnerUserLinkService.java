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
 *
 * Timing: works whether the parent already has a KittyP account when the clinic
 * registers them, or signs up months/years later with the same email/phone.
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
			// Already soft-linked — still ensure pets are on the user account.
			attachPetsToUser(owner, owner.getLinkedUser());
			return owner;
		}
		String email = normalizeEmail(owner.getEmail());
		String phone = normalizePhoneDigits(owner.getPhone());
		String altPhone = normalizePhoneDigits(owner.getAlternatePhone());

		User matched = null;
		if (email != null) {
			matched = userRepository.findByEmail(email).orElse(null);
		}
		if (matched == null) {
			matched = matchUniqueByPhone(phone);
		}
		if (matched == null) {
			matched = matchUniqueByPhone(altPhone);
		}
		if (matched != null) {
			owner.setLinkedUser(matched);
			owner = clinicPetOwnerRepository.save(owner);
			attachPetsToUser(owner, matched);
			log.info("Linked clinic owner {} to existing user {} (pets attached)", owner.getUuid(),
					matched.getUuid());
		}
		return owner;
	}

	/**
	 * Called on parent signup / profile update. Finds unmatched clinic owners by
	 * email or phone, links them, and attaches pets. Also re-attaches pets for
	 * owners already soft-linked to this user (e.g. clinic created record while
	 * account existed but pets were not yet on the parent profile).
	 */
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
		List<ClinicPetOwner> byAltPhone = phone == null || !phone.matches("\\d{10}")
				? List.of()
				: clinicPetOwnerRepository.findByLinkedUserIsNullAndIsActiveTrueAndAlternatePhone(phone);

		Set<Long> emailIds = byEmail.stream().map(ClinicPetOwner::getId).collect(Collectors.toSet());
		Set<Long> phoneIds = Stream.concat(byPhone.stream(), byAltPhone.stream())
				.map(ClinicPetOwner::getId)
				.collect(Collectors.toSet());

		Set<String> emails = Stream.of(byEmail, byPhone, byAltPhone)
				.flatMap(List::stream)
				.map(o -> normalizeEmail(o.getEmail()))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<String> phones = Stream.of(byEmail, byPhone, byAltPhone)
				.flatMap(List::stream)
				.map(o -> normalizePhoneDigits(o.getPhone()))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (!emailIds.isEmpty() && !phoneIds.isEmpty()) {
			Set<Long> intersection = new HashSet<>(emailIds);
			intersection.retainAll(phoneIds);
			if (intersection.isEmpty() && emails.size() > 1 && phones.size() > 1) {
				log.warn("Ambiguous clinic-owner match for user {} (email vs phone conflict) — skipping auto-link",
						user.getUuid());
				// Still attach pets for already-linked owners below.
			} else {
				linkUnmatchedOwners(user, Stream.of(byEmail, byPhone, byAltPhone)
						.flatMap(List::stream)
						.collect(Collectors.toMap(ClinicPetOwner::getId, o -> o, (a, b) -> a))
						.values());
			}
		} else {
			linkUnmatchedOwners(user, Stream.of(byEmail, byPhone, byAltPhone)
					.flatMap(List::stream)
					.collect(Collectors.toMap(ClinicPetOwner::getId, o -> o, (a, b) -> a))
					.values());
		}

		// Edge case: already soft-linked earlier without pets on the user account.
		int attached = ensurePetsForAlreadyLinkedOwners(user);
		return attached;
	}

	private void linkUnmatchedOwners(User user, Iterable<ClinicPetOwner> owners) {
		for (ClinicPetOwner owner : owners) {
			if (owner.getLinkedUser() != null) {
				continue;
			}
			owner.setLinkedUser(user);
			clinicPetOwnerRepository.save(owner);
			attachPetsToUser(owner, user);
			log.info("Late-linked clinic owner {} to user {}", owner.getUuid(), user.getUuid());
		}
	}

	private int ensurePetsForAlreadyLinkedOwners(User user) {
		List<ClinicPetOwner> already = clinicPetOwnerRepository.findByLinkedUser_IdAndIsActiveTrue(user.getId());
		int n = 0;
		for (ClinicPetOwner owner : already) {
			int before = userPetCount(user);
			attachPetsToUser(owner, user);
			if (userPetCount(user) > before) {
				n++;
			}
		}
		return n;
	}

	private int userPetCount(User user) {
		User managed = userDao.userByUuid(user.getUuid());
		return managed.getPets() == null ? 0 : managed.getPets().size();
	}

	private User matchUniqueByPhone(String phone) {
		if (phone == null || !phone.matches("\\d{10}")) {
			return null;
		}
		List<User> byPhone = userRepository.findByPhoneDigits(phone);
		if (byPhone.size() == 1) {
			return byPhone.get(0);
		}
		if (byPhone.size() > 1) {
			log.warn("Ambiguous phone match {} — skipping link", phone);
		}
		return null;
	}

	/** Attach clinic-registered pets (already in {@code pets}) to the platform user. */
	private void attachPetsToUser(ClinicPetOwner owner, User user) {
		List<Pet> clinicPets = petsRepository.findByClinicOwner_IdAndIsActiveTrue(owner.getId());
		User managed = userDao.userByUuid(user.getUuid());
		Set<String> owned = managed.getPets() == null ? Set.of()
				: managed.getPets().stream().map(Pet::getUuid).collect(Collectors.toSet());
		boolean changed = false;
		for (Pet pet : clinicPets) {
			if (Boolean.TRUE.equals(pet.getHiddenFromParent())) {
				continue;
			}
			if (owned.contains(pet.getUuid())) {
				continue;
			}
			managed.addPet(pet);
			changed = true;
		}
		if (changed) {
			userDao.saveUser(managed);
			log.info("Attached {} clinic pet(s) to user {}", clinicPets.size(), managed.getUuid());
		}
	}
}
