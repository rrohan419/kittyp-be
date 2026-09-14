package com.kittyp.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.auth.dto.MasterTotpEnrollmentModel;
import com.kittyp.auth.service.MasterTotpService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AdminMasterTotpController {

	private final MasterTotpService masterTotpService;
	private final ApiResponse<?> responseBuilder;

	@GetMapping(ApiUrl.ADMIN_MASTER_TOTP)
	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
	public ResponseEntity<SuccessResponse<MasterTotpEnrollmentModel>> enrollment() {
		return responseBuilder.buildSuccessResponse(
				masterTotpService.enrollment(), ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
