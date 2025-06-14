/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.models.UserDetailsModel;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private final Mapper mapper;
	private final RoleDao roleDao;
	private final VerificationCodeService verificationCodeService;
	private final PasswordEncoder encoder;
	private final ZeptoMailService zeptoMailService;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public UserDetailsModel userDetailsByEmail(String email) {
		User user = userDao.userByEmail(email);
		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));
		
		return userDetailsModel;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public void addRoleAdminToUser(String uuid) {
		User user = userDao.userByUuid(uuid);
		
		user.addRole(roleDao.roleByName(ERole.ROLE_ADMIN));
		
		userDao.saveUser(user);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public UserDetailsModel updateUserDetail(String email, UserDetailDto userDetailDto) {
		User user = userDao.userByEmail(email);
		
		if(userDetailDto.getEmail() != null && !userDetailDto.getEmail().isBlank()) {
			user.setEmail(userDetailDto.getEmail());
		}
		
		if(userDetailDto.getFirstName() != null && !userDetailDto.getFirstName().isBlank()) {
			user.setFirstName(userDetailDto.getFirstName());
		}
		if(userDetailDto.getLastName() != null && !userDetailDto.getLastName().isBlank()) {
			user.setLastName(userDetailDto.getLastName());
		}
		
		
		user = userDao.saveUser(user);
		
		return mapper.convert(user, UserDetailsModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public boolean updatePassword(UpdatePasswordDto updatePasswordDto) {
		User user = userDao.userByEmail(updatePasswordDto.getEmail());
		boolean verified = verificationCodeService.verifyCode(user.getUuid(), updatePasswordDto.getCode(), true);
		
		if(verified) {
		user.setPassword(encoder.encode(updatePasswordDto.getPassword()));
		userDao.saveUser(user);
		return true;
	}
	return false;
	}
	


	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public boolean sendResetPasswordCode(String email) {
		User user = userDao.userByEmail(email);
		zeptoMailService.sendPasswordResetCode(user.getEmail());
		return true;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public boolean verifyResetPasswordCode(String code, String email) {
		User user = userDao.userByEmail(email);
		return verificationCodeService.verifyCode(user.getUuid(), code, false);
		

	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public PaginationModel<UserDetailsModel> getAllUsers(Integer pageNumber, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
		Page<User> userPage = userDao.findAllUsers(pageable);
		
		List<UserDetailsModel> userModels = userPage.getContent().stream()
			.map(user -> {
				UserDetailsModel model = mapper.convert(user, UserDetailsModel.class);
				model.setRoles(user.getUserRoles().stream()
					.map(UserRole::getRole)
					.map(role -> role.getName().name())
					.collect(Collectors.toSet()));
				return model;
			})
			.collect(Collectors.toList());
		
		return userPageToModel(new PageImpl<>(userModels, pageable, userPage.getTotalElements()));
	}

	private PaginationModel<UserDetailsModel> userPageToModel(Page<UserDetailsModel> userPage) {
		PaginationModel<UserDetailsModel> paginationModel = new PaginationModel<>();
		paginationModel.setModels(userPage.getContent());
		paginationModel.setIsFirst(userPage.isFirst());
		paginationModel.setIsLast(userPage.isLast());
		paginationModel.setTotalElements(userPage.getTotalElements());
		paginationModel.setTotalPages(userPage.getTotalPages());
		return paginationModel;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public UserDetailsModel updateUserStatus(String userUuid, boolean enabled) {
		User user = userDao.userByUuid(userUuid);
		user.setEnabled(enabled);
		user.setIsActive(enabled);
		user = userDao.saveUser(user);
		
		UserDetailsModel userDetailsModel = mapper.convert(user, UserDetailsModel.class);
		userDetailsModel.setRoles(user.getUserRoles().stream()
				.map(UserRole::getRole)
				.map(role -> role.getName().name())
				.collect(Collectors.toSet()));
		
		return userDetailsModel;
	}

}
