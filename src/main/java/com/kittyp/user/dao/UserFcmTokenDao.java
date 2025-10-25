package com.kittyp.user.dao;

import java.util.List;
import java.util.Optional;

import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;

public interface UserFcmTokenDao {
    
    List<UserFcmToken> findByUser(User user);
    Optional<UserFcmToken> findByTokenAndUser(String token, User user);
    void deleteByToken(String token);
    UserFcmToken savFcmToken(UserFcmToken userFcmToken);

    Optional<UserFcmToken> findByActiveAndUser(Boolean isActive, User user);
}
