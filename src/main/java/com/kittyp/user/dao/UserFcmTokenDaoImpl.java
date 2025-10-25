package com.kittyp.user.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;
import com.kittyp.user.repository.UserFcmTokenRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class UserFcmTokenDaoImpl implements UserFcmTokenDao{

    private final Environment env;
    private final UserFcmTokenRepository fcmTokenRepository;
    @Override
    public List<UserFcmToken> findByUser(User user) {
        try {
			return fcmTokenRepository.findByUser(user);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public Optional<UserFcmToken> findByTokenAndUser(String token, User user) {
        try {
			return fcmTokenRepository.findByTokenAndUser(token, user);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public void deleteByToken(String token) {
        try {
			fcmTokenRepository.deleteByToken(token);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public UserFcmToken savFcmToken(UserFcmToken userFcmToken) {
        try {
			return fcmTokenRepository.save(userFcmToken);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public Optional<UserFcmToken> findByActiveAndUser(Boolean isActive, User user) {
        try {
			return fcmTokenRepository.findByActiveAndUser(isActive, user);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }
    
}
