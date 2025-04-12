/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.auth.service.AuthService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.LoginRequestDto;
import com.kittyp.common.dto.SignupRequestDto;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.JwtResponseModel;
import com.kittyp.common.model.MessageResponse;

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
	private final ApiResponse responseBuilder;
	
	@PostMapping(ApiUrl.SIGNIN)
    public ResponseEntity<SuccessResponse<JwtResponseModel>> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
		
		JwtResponseModel response = authService.loginUser(loginRequest);
		
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
	
	@PostMapping(ApiUrl.SIGNUP)
    public ResponseEntity<SuccessResponse<MessageResponse>> registerUser(@Valid @RequestBody SignupRequestDto signUpRequest) {
		MessageResponse response =  authService.registerUser(signUpRequest);
		
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
