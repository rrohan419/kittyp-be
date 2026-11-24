package com.kittyp.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.notification.enums.MessageTenantType;
import com.kittyp.notification.model.MessagingDtos.*;
import com.kittyp.notification.service.MessagingLifecycleService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL + ApiUrl.CLINIC_MESSAGING)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN','ROLE_CLINIC_STAFF')")
public class ClinicMessagingController {
    private final MessagingLifecycleService service;
    private final ClinicService clinicService;
    private final ClinicRepository clinicRepository;
    private final ApiResponse<?> response;

    @GetMapping("/overview")
    public ResponseEntity<SuccessResponse<Overview>> overview(@PathVariable String clinicUuid) {
        return ok(service.overview(MessageTenantType.CLINIC, tenantId(clinicUuid)));
    }

    @GetMapping("/messages")
    public ResponseEntity<SuccessResponse<java.util.List<MessageModel>>> list(
            @PathVariable String clinicUuid, @RequestParam(defaultValue = "history") String view) {
        return ok(service.list(MessageTenantType.CLINIC, tenantId(clinicUuid), view));
    }

    @PostMapping("/messages")
    public ResponseEntity<SuccessResponse<java.util.List<MessageModel>>> create(@PathVariable String clinicUuid,
            @RequestBody @jakarta.validation.Valid CreateMessageRequest request) {
        return ok(service.createBatch(MessageTenantType.CLINIC, tenantId(clinicUuid), request, email()),
                HttpStatus.CREATED);
    }

    @GetMapping("/messages/{uuid}")
    public ResponseEntity<SuccessResponse<MessageModel>> get(
            @PathVariable String clinicUuid, @PathVariable String uuid) {
        return ok(service.get(MessageTenantType.CLINIC, tenantId(clinicUuid), uuid));
    }

    @PatchMapping("/messages/{uuid}")
    public ResponseEntity<SuccessResponse<MessageModel>> patch(
            @PathVariable String clinicUuid, @PathVariable String uuid,
            @RequestBody @jakarta.validation.Valid PatchMessageRequest request) {
        return ok(service.patch(MessageTenantType.CLINIC, tenantId(clinicUuid), uuid, request, email()));
    }

    @PostMapping("/messages/{uuid}/{action}")
    public ResponseEntity<SuccessResponse<MessageModel>> action(
            @PathVariable String clinicUuid, @PathVariable String uuid, @PathVariable String action) {
        return ok(service.action(MessageTenantType.CLINIC, tenantId(clinicUuid), uuid, action, email()));
    }

    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN')")
    @PostMapping("/pause-all")
    public ResponseEntity<SuccessResponse<SettingsModel>> pause(@PathVariable String clinicUuid) {
        return ok(service.setPause(MessageTenantType.CLINIC, tenantId(clinicUuid), true, email()));
    }

    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN')")
    @PostMapping("/resume-all")
    public ResponseEntity<SuccessResponse<SettingsModel>> resume(@PathVariable String clinicUuid) {
        return ok(service.setPause(MessageTenantType.CLINIC, tenantId(clinicUuid), false, email()));
    }

    @GetMapping("/automations")
    public ResponseEntity<SuccessResponse<java.util.List<AutomationItem>>> automations(
            @PathVariable String clinicUuid) {
        return ok(service.getAutomations(MessageTenantType.CLINIC, tenantId(clinicUuid)).automations());
    }

    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN')")
    @PutMapping("/automations")
    public ResponseEntity<SuccessResponse<java.util.List<AutomationItem>>> automations(
            @PathVariable String clinicUuid,
            @RequestBody @jakarta.validation.Valid java.util.List<@jakarta.validation.Valid AutomationItem> request) {
        java.util.Map<String, Boolean> enabled = request.stream().collect(java.util.stream.Collectors.toMap(
                AutomationItem::id, AutomationItem::enabled, (a, b) -> b));
        return ok(
                service.setAutomations(MessageTenantType.CLINIC, tenantId(clinicUuid), enabled, email()).automations());
    }

    @GetMapping("/settings")
    public ResponseEntity<SuccessResponse<SettingsModel>> settings(@PathVariable String clinicUuid) {
        return ok(service.getSettings(MessageTenantType.CLINIC, tenantId(clinicUuid)));
    }

    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN')")
    @PutMapping("/settings")
    public ResponseEntity<SuccessResponse<SettingsModel>> settings(
            @PathVariable String clinicUuid, @RequestBody @jakarta.validation.Valid SettingsRequest request) {
        return ok(service.setPause(MessageTenantType.CLINIC, tenantId(clinicUuid), request.effectivePaused(), email()));
    }

    @GetMapping("/credits")
    public ResponseEntity<SuccessResponse<CreditsModel>> credits(@PathVariable String clinicUuid) {
        return ok(service.credits(MessageTenantType.CLINIC, tenantId(clinicUuid)));
    }

    @GetMapping("/audit")
    public ResponseEntity<SuccessResponse<java.util.List<AuditModel>>> audit(@PathVariable String clinicUuid) {
        return ok(service.auditEvents(MessageTenantType.CLINIC, tenantId(clinicUuid)));
    }

    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN')")
    @PostMapping("/credits/purchase")
    public ResponseEntity<SuccessResponse<PurchaseModel>> purchase(
            @PathVariable String clinicUuid, @RequestBody @jakarta.validation.Valid PurchaseRequest request) {
        return ok(service.createPurchase(MessageTenantType.CLINIC, tenantId(clinicUuid), request.effectiveChannel(),
                request.bundleId()), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_CLINIC_ADMIN')")
    @PostMapping("/credits/purchase/verify")
    public ResponseEntity<SuccessResponse<PurchaseModel>> verify(
            @PathVariable String clinicUuid, @RequestBody @jakarta.validation.Valid VerifyPurchaseRequest request) {
        return ok(service.verifyPurchase(MessageTenantType.CLINIC, tenantId(clinicUuid), request, email()));
    }

    private Long tenantId(String clinicUuid) {
        clinicService.get(clinicUuid, email()); // Clinic membership/ownership authorization is mandatory.
        return clinicRepository.findByUuid(clinicUuid).getId();
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> ResponseEntity<SuccessResponse<T>> ok(T data) {
        return ok(data, HttpStatus.OK);
    }

    private <T> ResponseEntity<SuccessResponse<T>> ok(T data, HttpStatus status) {
        return response.buildSuccessResponse(data, ResponseMessage.SUCCESS, status);
    }
}
