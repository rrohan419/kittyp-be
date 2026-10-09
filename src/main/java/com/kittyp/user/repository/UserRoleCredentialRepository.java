package com.kittyp.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.UserRoleCredential;

public interface UserRoleCredentialRepository extends JpaRepository<UserRoleCredential, Long> {

	List<UserRoleCredential> findByUser_Id(Long userId);

	boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);
}
