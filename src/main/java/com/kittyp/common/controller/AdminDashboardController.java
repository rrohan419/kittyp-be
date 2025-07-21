package com.kittyp.common.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.AdminDashboardResponse;
import com.kittyp.common.service.AdminService;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AdminDashboardController {
    
    private final ApiResponse<?> responseBuilder;
    private final AdminService adminService;

@GetMapping("/admin/dashboard-summary")
@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
public ResponseEntity<SuccessResponse<AdminDashboardResponse>> getAdminDashboardSummary() {
    AdminDashboardResponse summary = adminService.getAdminDashboardData();

    return responseBuilder.buildSuccessResponse(summary, ResponseMessage.SUCCESS, HttpStatus.OK);
}
}
