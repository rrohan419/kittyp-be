package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRoleCredential;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.UserRoleCredentialRepository;

class RoleCredentialServiceTest {

	private UserRoleCredentialRepository repository;
	private PasswordEncoder encoder;
	private RoleCredentialService service;

	@BeforeEach
	void setUp() {
		repository = org.mockito.Mockito.mock(UserRoleCredentialRepository.class);
		encoder = org.mockito.Mockito.mock(PasswordEncoder.class);
		service = new RoleCredentialService(repository, encoder);
	}

	@Test
	void store_savesBcryptHashAndNotTheRawPassword() {
		when(encoder.encode("RolePass1!")).thenReturn("$2a$hash");
		when(repository.existsByUser_IdAndRole_Id(3L, 9L)).thenReturn(false);
		User user = userWithRole();

		service.storeNewRolePassword(user, ERole.ROLE_DOCTOR, "RolePass1!");

		ArgumentCaptor<UserRoleCredential> saved = ArgumentCaptor.forClass(UserRoleCredential.class);
		verify(repository).save(saved.capture());
		assertEquals("$2a$hash", saved.getValue().getPasswordHash());
		assertEquals(false, "RolePass1!".equals(saved.getValue().getPasswordHash()));
	}

	@Test
	void store_existingCredentialIsNotReplaced() {
		when(repository.existsByUser_IdAndRole_Id(3L, 9L)).thenReturn(true);

		service.storeNewRolePassword(userWithRole(), ERole.ROLE_DOCTOR, "RolePass1!");

		verify(encoder, never()).encode(any());
		verify(repository, never()).save(any());
	}

	private static User userWithRole() {
		User user = User.builder().email("ada@example.com").password("account-hash").build();
		user.setId(3L);
		Role role = new Role();
		role.setId(9L);
		role.setName(ERole.ROLE_DOCTOR);
		user.addRole(role);
		return user;
	}
}
