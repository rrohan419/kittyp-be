package com.kittyp.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long>{
    
    List<UserFcmToken> findByUser(User user);
    Optional<UserFcmToken> findByTokenAndUser(String token, User user);
    void deleteByToken(String token);
    Optional<UserFcmToken> findByActiveAndUser(Boolean isActive, User user);
}
