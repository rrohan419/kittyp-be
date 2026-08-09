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

import java.util.Map;

import com.kittyp.auth.dto.SignupOtpSendRequest;
import com.kittyp.auth.dto.SignupOtpVerifyRequest;
import com.kittyp.auth.dto.SocialSso;
import com.kittyp.auth.service.AuthService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.user.dto.UpdatePasswordDto;
import com.kittyp.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
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
			@Valid @RequestBody LoginRequestDto loginRequest,
			HttpServletRequest request) {

		JwtResponseModel response = authService.loginUser(loginRequest, clientIp(request));

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SIGNUP)
	public ResponseEntity<SuccessResponse<MessageResponse>> registerUser(
			@Valid @RequestBody SignupRequestDto signUpRequest) {
		MessageResponse response = authService.registerUser(signUpRequest);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SIGNUP_DOCTOR)
	public ResponseEntity<SuccessResponse<MessageResponse>> registerDoctor(
			@Valid @RequestBody SignupDoctorRequestDto signUpRequest) {
		MessageResponse response = authService.registerDoctor(signUpRequest);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SIGNUP_CLINIC)
	public ResponseEntity<SuccessResponse<MessageResponse>> registerClinic(
			@Valid @RequestBody SignupClinicRequestDto signUpRequest) {
		MessageResponse response = authService.registerClinic(signUpRequest);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SIGNUP_OTP_SEND)
	public ResponseEntity<SuccessResponse<MessageResponse>> sendSignupOtp(
			@Valid @RequestBody SignupOtpSendRequest request) {
		return responseBuilder.buildSuccessResponse(authService.sendSignupOtp(request), ResponseMessage.SUCCESS,
				HttpStatus.OK);
	}

	@PostMapping(ApiUrl.SIGNUP_OTP_VERIFY)
	public ResponseEntity<SuccessResponse<Map<String, Boolean>>> verifySignupOtp(
			@Valid @RequestBody SignupOtpVerifyRequest request) {
		return responseBuilder.buildSuccessResponse(authService.verifySignupOtp(request), ResponseMessage.SUCCESS,
				HttpStatus.OK);
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
	public ResponseEntity<SuccessResponse<Boolean>> updatePassword(
			@Valid @RequestBody UpdatePasswordDto updatePasswordDto) {

		Boolean response = userService.updatePassword(updatePasswordDto);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.SEND_CODE)
	public ResponseEntity<SuccessResponse<Boolean>> sendResetPasswordCode(@RequestParam String email) {
		// Always return success to prevent email enumeration
		userService.sendResetPasswordCode(email);
		return responseBuilder.buildSuccessResponse(true, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	private static String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
