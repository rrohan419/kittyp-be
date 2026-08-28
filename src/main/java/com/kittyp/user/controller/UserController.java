/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.controller;

import java.util.List;

import com.kittyp.clinic.dto.ClinicDtos.SwitchClinicRequest;
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

import com.kittyp.auth.util.SecurityContextUtils;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.user.dto.UserDetailDto;
import com.kittyp.user.dto.ProfileOtpSendRequest;
import com.kittyp.user.dto.ProfileOtpVerifyRequest;
import com.kittyp.user.dto.ProfilePictureUpdateDto;
import com.kittyp.user.dto.UserStatusUpdateDto;
import com.kittyp.user.models.FcmTokenModel;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.user.models.UserDetailsModel;
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
public class UserController {

    private final UserService userService;
    private final ClinicService clinicService;
    private final ApiResponse<?> responseBuilder;
    private final SecurityContextUtils securityContextUtils;

    @GetMapping(ApiUrl.USER_DETAILS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> getUserDetails() {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserDetailsModel response = userService.userDetailsByEmail(email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.USER_CLINICS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<ClinicModel>>> userClinics() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(clinicService.mine(email), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_SWITCH_CLINIC)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<ClinicModel>> switchClinic(@RequestBody @Valid SwitchClinicRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(clinicService.switchClinic(request.clinicUuid(), email),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_BASE_URL)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> updateUserDetails(
            @RequestParam(required = false) String userUuid, @RequestBody UserDetailDto userDetailDto) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userUuid != null) {
            securityContextUtils.requireSelfOrAdmin(userUuid);
        }

        UserDetailsModel response = userService.updateUserDetail(email, userDetailDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_PROFILE_OTP_SEND)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<MessageResponse>> sendProfileOtp(
            @RequestBody @Valid ProfileOtpSendRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(userService.sendProfileOtp(email, request),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_PROFILE_OTP_VERIFY)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<java.util.Map<String, Boolean>>> verifyProfileOtp(
            @RequestBody @Valid ProfileOtpVerifyRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(userService.verifyProfileOtp(email, request),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping("/user/admin")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<String>> assignRoleAdmin(@RequestParam String userUuid) {
        userService.addRoleAdminToUser(userUuid);
        return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping("/admin/users")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<PaginationModel<UserDetailsModel>>> getAllUsers(
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "") String q) {
        PaginationModel<UserDetailsModel> response = userService.getAllUsers(pageNumber, pageSize, q);
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

    @PatchMapping("/user/profile-picture")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<UserDetailsModel>> updateUserProfilePicture(
            @RequestParam String userUuid,
            @RequestBody ProfilePictureUpdateDto profilePictureUpdateDto) {
        securityContextUtils.requireSelfOrAdmin(userUuid);
        UserDetailsModel updatedUser = userService.updateUserProfile(userUuid,
                profilePictureUpdateDto.getProfilePictureUrl());
        return responseBuilder.buildSuccessResponse(updatedUser, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping("/user/{fcmToken}")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<FcmTokenModel>> addFcmToken(
            @PathVariable String fcmToken,
            HttpServletRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        FcmTokenModel response = userService.updateUserFcmToken(email, fcmToken, request);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping("/user/test/push")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<String>> testPushNotification(
            @RequestParam String title,
            @RequestParam String body,
            @RequestParam String email) {

        userService.sendPushNotification(email, title, body);
        return responseBuilder.buildSuccessResponse("", ResponseMessage.SUCCESS, HttpStatus.OK);
    }

}
