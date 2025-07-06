package com.kittyp.auth.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kittyp.auth.config.UserDetailsImpl;
import com.kittyp.auth.dto.GoogleUserInfo;
import com.kittyp.auth.dto.SocialSso;
import com.kittyp.auth.util.JwtUtils;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.exception.ResourceAlreadyExistsException;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserDao userDao;
	private final PasswordEncoder encoder;
	private final RoleDao roleDao;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final ZeptoMailService zeptoMailService;
	private final GoogleOAuth2Service googleOAuth2Service;

	@Transactional
	@Override
	public MessageResponse registerUser(SignupRequestDto signupRequestDto) {

		if (userDao.userPresentByEmail(signupRequestDto.getEmail())) {
			throw new ResourceAlreadyExistsException("User", "email", signupRequestDto.getEmail());
		}

		// Create new user
		User user = User.builder()
				.uuid(UUID.randomUUID().toString())
				.email(signupRequestDto.getEmail()).password(encoder.encode(signupRequestDto.getPassword()))
				.firstName(signupRequestDto.getFirstName()).lastName(signupRequestDto.getLastName()).build();

		user = userDao.saveUser(user);

		// Assign roles
		Set<String> strRoles = signupRequestDto.getRoles();
		Set<UserRole> userRoles = new HashSet<>();

		if (strRoles == null || strRoles.isEmpty()) {
			Role userRole = roleDao.roleByName(ERole.ROLE_USER);
			if (userRole == null) {
				throw new RuntimeException("Error: Default ROLE_USER not found.");
			}
			userRoles.add(new UserRole(user, userRole));
		} else {
			for (String role : strRoles) {
				Role foundRole;
				switch (role.toLowerCase()) {
				case "admin":
					foundRole = roleDao.roleByName(ERole.ROLE_ADMIN);
					break;
				case "mod":
					foundRole = roleDao.roleByName(ERole.ROLE_MODERATOR);
					break;
				default:
					foundRole = roleDao.roleByName(ERole.ROLE_USER);
				}

				if (foundRole == null) {
					throw new RuntimeException("Error: Role not found for " + role);
				}
				userRoles.add(new UserRole(user, foundRole));
			}
		}

		user.getUserRoles().addAll(userRoles);
		userDao.saveUser(user);
		zeptoMailService.sendWelcomeEmail(user.getEmail());
		return new MessageResponse(ResponseMessage.USER_REGISTERED_SUCCESSFULLY);
	}

	@Override
	public JwtResponseModel loginUser(LoginRequestDto loginRequestDto) {
		
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

		return new JwtResponseModel(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
	}

	@Override
	@Transactional
	public JwtResponseModel googleUserSignin(SocialSso socialSso) {
		try {
			// Get user info directly from Google using access token
			GoogleUserInfo googleUserInfo = googleOAuth2Service.getUserInfo(socialSso.getToken());
			
			// Check if user exists by email
			User existingUser = null;
			try {
				existingUser = userDao.userByEmail(googleUserInfo.getEmail());
			} catch (Exception e) {
				// User doesn't exist, will create new one
			}
			
			if (existingUser == null) {
				// Create new user
				existingUser = User.builder()
						.uuid(UUID.randomUUID().toString())
						.email(googleUserInfo.getEmail())
						.firstName(googleUserInfo.getGivenName())
						.lastName(googleUserInfo.getFamilyName())
						.password(encoder.encode(UUID.randomUUID().toString())) // Generate random password
						.enabled(true)
						.build();
				
				// Assign default role
				Role userRole = roleDao.roleByName(ERole.ROLE_USER);
				if (userRole == null) {
					throw new RuntimeException("Error: Default ROLE_USER not found.");
				}
				
				existingUser.addRole(userRole);
				existingUser = userDao.saveUser(existingUser);
				
				// Send welcome email
				zeptoMailService.sendWelcomeEmail(existingUser.getEmail());
			}
			
			// Create authentication token
			UserDetailsImpl userDetails = (UserDetailsImpl) UserDetailsImpl.build(existingUser);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
			
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);
			
			List<String> roles = userDetails.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.toList();
			
			return new JwtResponseModel(jwt, userDetails.getId(), userDetails.getUsername(), 
					userDetails.getEmail(), roles);
			
		} catch (Exception e) {
			throw new RuntimeException("Google authentication failed: " + e.getMessage(), e);
		}
	}

}
