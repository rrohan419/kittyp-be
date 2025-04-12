/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.user.models.UserDetailsModel;
import com.kittyp.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final ApiResponse responseBuilder;
	
	@GetMapping(ApiUrl.USER_DETAILS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> getUserDetails() {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        UserDetailsModel response = userService.userDetailsByEmail(email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@GetMapping("/test")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<SuccessResponse<UserDetailsModel>> test() {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        UserDetailsModel response = userService.userDetailsByEmail(email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
