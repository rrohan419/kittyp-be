package com.kittyp.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.AdminDashboardResponse;
import com.kittyp.common.model.HealthOptimizeRequest;
import com.kittyp.common.model.HealthOptimizeResponse;
import com.kittyp.common.model.SystemHealthResponse;
import com.kittyp.common.service.AdminService;
import com.kittyp.common.service.HealthLoadTestService;
import com.kittyp.common.service.HealthOptimizeService;
import com.kittyp.common.service.SystemHealthService;

import org.springframework.beans.factory.ObjectProvider;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ApiResponse<?> responseBuilder;
    private final AdminService adminService;
    private final SystemHealthService systemHealthService;
    private final HealthOptimizeService healthOptimizeService;
    private final ObjectProvider<HealthLoadTestService> loadTest;

    @GetMapping("/admin/dashboard-summary")
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<AdminDashboardResponse>> getAdminDashboardSummary() {
        AdminDashboardResponse summary = adminService.getAdminDashboardData();
        return responseBuilder.buildSuccessResponse(summary, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.ADMIN_SYSTEM_HEALTH)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<SystemHealthResponse>> systemHealth() {
        return responseBuilder.buildSuccessResponse(systemHealthService.snapshot(), ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PostMapping(ApiUrl.ADMIN_SYSTEM_HEALTH_OPTIMIZE)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<HealthOptimizeResponse>> optimize(
            @Valid @RequestBody HealthOptimizeRequest request) {
        return responseBuilder.buildSuccessResponse(healthOptimizeService.optimize(request.target()),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.ADMIN_SYSTEM_HEALTH_LOAD_START)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<HealthOptimizeResponse>> startLoad(
            @Valid @RequestBody HealthOptimizeRequest request) {
        HealthLoadTestService service = requireLoadTest();
        SystemHealthResponse before = systemHealthService.snapshot();
        String summary = service.start(request.target());
        SystemHealthResponse after = systemHealthService.snapshot();
        return responseBuilder.buildSuccessResponse(
                new HealthOptimizeResponse(request.target().name(), true, summary, "Stop load or wait 60s to recover.",
                        before, after),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.ADMIN_SYSTEM_HEALTH_LOAD_STOP)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<HealthOptimizeResponse>> stopLoad(
            @RequestBody(required = false) HealthOptimizeRequest request) {
        HealthLoadTestService service = requireLoadTest();
        SystemHealthResponse before = systemHealthService.snapshot();
        String summary = service.stop(request == null ? null : request.target());
        SystemHealthResponse after = systemHealthService.snapshot();
        String target = request == null || request.target() == null ? "ALL" : request.target().name();
        return responseBuilder.buildSuccessResponse(
                new HealthOptimizeResponse(target, true, summary, "Alerts clear when ratios drop below 70%.", before,
                        after),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private HealthLoadTestService requireLoadTest() {
        HealthLoadTestService service = loadTest.getIfAvailable();
        if (service == null) {
            throw new CustomException("Load test is disabled", HttpStatus.NOT_FOUND);
        }
        return service;
    }
}
