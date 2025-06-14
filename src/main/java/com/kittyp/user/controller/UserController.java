/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.controller;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.dto.UserStatusUpdateDto;
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
	private final Environment env;
	
	@GetMapping(ApiUrl.USER_DETAILS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> getUserDetails() {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        UserDetailsModel response = userService.userDetailsByEmail(email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@PostMapping(ApiUrl.USER_BASE_URL)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> updateUserDetails(@RequestParam(required = false) String userUuid, @RequestBody UserDetailDto userDetailDto) {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        UserDetailsModel response = userService.updateUserDetail(email, userDetailDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@GetMapping("/test")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> test() {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        UserDetailsModel response = userService.userDetailsByEmail(email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@PatchMapping("/user/admin")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<String>> assignRoleAdmin(@RequestParam String key, @RequestParam String userUuid) {
        
		if(!key.equals(env.getProperty(KeyConstant.SECRET_KEY))) {
			throw new CustomException("secret key did not match", HttpStatus.UNAUTHORIZED);
		}
		
//		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        userService.addRoleAdminToUser(userUuid);
        return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    // Updated admin endpoints
    
    @GetMapping("/admin/users")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<PaginationModel<UserDetailsModel>>> getAllUsers(
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PaginationModel<UserDetailsModel> response = userService.getAllUsers(pageNumber, pageSize);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping("/admin/users/{userUuid}/status")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> updateUserStatus(
            @PathVariable String userUuid,
            @RequestBody UserStatusUpdateDto statusUpdateDto) {
        UserDetailsModel updatedUser = userService.updateUserStatus(userUuid, statusUpdateDto.isEnabled());
        return responseBuilder.buildSuccessResponse(updatedUser, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
