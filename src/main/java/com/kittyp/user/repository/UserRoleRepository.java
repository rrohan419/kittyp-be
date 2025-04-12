package com.kittyp.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long>{

	List<UserRole> findByUserId(Long userId);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
