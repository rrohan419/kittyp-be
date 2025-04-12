package com.kittyp.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.Role;
import com.kittyp.user.enums.ERole;

public interface RoleRepository extends JpaRepository<Role, Long>{

	Optional<Role> findByName(ERole name);
}
