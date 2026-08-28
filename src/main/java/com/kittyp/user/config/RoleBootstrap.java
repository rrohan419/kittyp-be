package com.kittyp.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.kittyp.user.entity.Role;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ensures every {@link ERole} exists in the roles table on startup.
 * Required for doctor/clinic signup after CRM roles were added to the enum.
 */
@Component
@RequiredArgsConstructor
public class RoleBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(RoleBootstrap.class);

	private final RoleRepository roleRepository;

	@Override
	public void run(ApplicationArguments args) {
		for (ERole roleName : ERole.values()) {
			roleRepository.findByName(roleName).orElseGet(() -> {
				Role role = new Role();
				role.setName(roleName);
				role.setIsActive(true);
				Role saved = roleRepository.save(role);
				log.info("Seeded missing role: {}", roleName);
				return saved;
			});
		}
	}
}
