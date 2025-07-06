/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.auth.dto.SocialSso;
import com.kittyp.auth.service.AuthService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final ApiResponse<?> responseBuilder;
	private final UserService userService;

	@PostMapping(ApiUrl.SIGNIN)
	public ResponseEntity<SuccessResponse<JwtResponseModel>> authenticateUser(
			@Valid @RequestBody LoginRequestDto loginRequest) {

		JwtResponseModel response = authService.loginUser(loginRequest);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SIGNUP)
	public ResponseEntity<SuccessResponse<MessageResponse>> registerUser(
			@Valid @RequestBody SignupRequestDto signUpRequest) {
		MessageResponse response = authService.registerUser(signUpRequest);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SOCIAL_SSO)
	public ResponseEntity<SuccessResponse<JwtResponseModel>> socialSso(@RequestBody SocialSso socialSso) {
		JwtResponseModel response = authService.googleUserSignin(socialSso);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.VERIFY_CODE)
	public ResponseEntity<SuccessResponse<Boolean>> verifyRestCode(@RequestParam String code,
			@RequestParam String email) {
		Boolean response = userService.verifyResetPasswordCode(code, email);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.USER_PASSWORD_RESET)
	public ResponseEntity<SuccessResponse<Boolean>> updatePassword(@RequestBody UpdatePasswordDto updatePasswordDto) {

		Boolean response = userService.updatePassword(updatePasswordDto);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.SEND_CODE)
	public ResponseEntity<SuccessResponse<Boolean>> sendResetPasswordCode(@RequestParam String email) {
		userService.sendResetPasswordCode(email);

		return responseBuilder.buildSuccessResponse(true, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
