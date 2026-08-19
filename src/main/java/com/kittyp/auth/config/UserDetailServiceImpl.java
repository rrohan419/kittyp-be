package com.kittyp.auth.config;

import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;
	private final DoctorProfileRepository doctorProfileRepository;
	private final ClinicRepository clinicRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = resolveUser(username)
				.orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

		Boolean active = user.getIsActive();
		if (active != null && !active) {
			throw new DisabledException("User account is not active. Please contact support.");
		}

		return UserDetailsImpl.build(user);
	}

	private Optional<User> resolveUser(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String key = raw.trim();
		if (key.indexOf('@') >= 0) {
			Optional<User> byEmail = userRepository.findByEmail(key);
			if (byEmail.isPresent()) {
				return byEmail;
			}
			return userRepository.findByEmail(key.toLowerCase());
		}
		Optional<User> byUserId = findUserByUuid(key);
		if (byUserId.isPresent()) {
			return byUserId;
		}
		Optional<User> byDoctor = findUserByDoctorUuid(key);
		if (byDoctor.isPresent()) {
			return byDoctor;
		}
		return findUserByClinicUuid(key);
	}

	private Optional<User> findUserByUuid(String key) {
		Optional<User> hit = userRepository.findByUuid(key);
		if (hit.isEmpty() && !key.equals(key.toUpperCase())) {
			hit = userRepository.findByUuid(key.toUpperCase());
		}
		return hit;
	}

	private Optional<User> findUserByDoctorUuid(String key) {
		Optional<DoctorProfile> doctor = doctorProfileRepository.findByUuid(key);
		if (doctor.isEmpty() && !key.equals(key.toUpperCase())) {
			doctor = doctorProfileRepository.findByUuid(key.toUpperCase());
		}
		return doctor.map(DoctorProfile::getUser);
	}

	private Optional<User> findUserByClinicUuid(String key) {
		Clinic clinic = clinicRepository.findByUuidFetchOwner(key);
		if (clinic == null && !key.equals(key.toUpperCase())) {
			clinic = clinicRepository.findByUuidFetchOwner(key.toUpperCase());
		}
		if (clinic == null || clinic.getOwner() == null) {
			return Optional.empty();
		}
		return Optional.of(clinic.getOwner());
	}

}
