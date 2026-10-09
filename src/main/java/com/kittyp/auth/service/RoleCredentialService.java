package com.kittyp.auth.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.entity.UserRoleCredential;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.UserRoleCredentialRepository;

/**
 * Stores and checks per-role bcrypt hashes. Does not read or write users.password.
 */
@Service
public class RoleCredentialService {

	private final UserRoleCredentialRepository repository;
	private final PasswordEncoder encoder;

	public RoleCredentialService(UserRoleCredentialRepository repository, PasswordEncoder encoder) {
		this.repository = repository;
		this.encoder = encoder;
	}

	/**
	 * Inserts a hash when this user does not already have one for the role.
	 * A blank password or an existing row is left unchanged.
	 */
	public void storeNewRolePassword(User user, ERole role, String rawPassword) {
		if (user == null || user.getId() == null || role == null || rawPassword == null || rawPassword.isBlank()) {
			return;
		}
		if (!isSelfService(role)) {
			return;
		}
		Role roleEntity = roleOn(user, role);
		if (roleEntity == null || roleEntity.getId() == null) {
			return;
		}
		if (repository.existsByUser_IdAndRole_Id(user.getId(), roleEntity.getId())) {
			return;
		}
		UserRoleCredential row = new UserRoleCredential();
		row.setUser(user);
		row.setRole(roleEntity);
		row.setPasswordHash(encoder.encode(rawPassword));
		repository.save(row);
	}

	public List<ERole> matchingRoles(Long userId, String rawPassword) {
		if (userId == null || rawPassword == null || rawPassword.isBlank()) {
			return List.of();
		}
		List<ERole> hits = new ArrayList<>();
		for (UserRoleCredential row : repository.findByUser_Id(userId)) {
			if (row.getRole() == null || row.getRole().getName() == null || row.getPasswordHash() == null) {
				continue;
			}
			if (encoder.matches(rawPassword, row.getPasswordHash())) {
				hits.add(row.getRole().getName());
			}
		}
		return hits;
	}

	private static boolean isSelfService(ERole role) {
		return role == ERole.ROLE_USER || role == ERole.ROLE_DOCTOR || role == ERole.ROLE_CLINIC_ADMIN;
	}

	private static Role roleOn(User user, ERole role) {
		if (user.getUserRoles() == null) {
			return null;
		}
		for (UserRole userRole : user.getUserRoles()) {
			if (userRole.getRole() != null && userRole.getRole().getName() == role) {
				return userRole.getRole();
			}
		}
		return null;
	}
}
