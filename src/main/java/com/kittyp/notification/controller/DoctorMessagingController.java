package com.kittyp.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.notification.enums.MessageTenantType;
import com.kittyp.notification.model.MessagingDtos.*;
import com.kittyp.notification.service.MessagingLifecycleService;
import com.kittyp.user.dao.UserDao;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL + ApiUrl.DOCTOR_MESSAGING)
@RequiredArgsConstructor
@PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
public class DoctorMessagingController {
    private final MessagingLifecycleService service;
    private final DoctorProfileDao doctorProfiles;
    private final UserDao userDao;
    private final ApiResponse<?> response;

    @GetMapping("/overview") public ResponseEntity<SuccessResponse<Overview>> overview() {
        return ok(service.overview(MessageTenantType.DOCTOR,tenantId()));
    }
    @GetMapping("/messages") public ResponseEntity<SuccessResponse<java.util.List<MessageModel>>> list(
            @RequestParam(defaultValue="history") String view) {
        return ok(service.list(MessageTenantType.DOCTOR,tenantId(),view));
    }
    @PostMapping("/messages") public ResponseEntity<SuccessResponse<java.util.List<MessageModel>>> create(
            @RequestBody @jakarta.validation.Valid CreateMessageRequest request) {
        return ok(service.createBatch(MessageTenantType.DOCTOR,tenantId(),request,email()),HttpStatus.CREATED);
    }
    @GetMapping("/messages/{uuid}") public ResponseEntity<SuccessResponse<MessageModel>> get(@PathVariable String uuid) {
        return ok(service.get(MessageTenantType.DOCTOR,tenantId(),uuid));
    }
    @PatchMapping("/messages/{uuid}") public ResponseEntity<SuccessResponse<MessageModel>> patch(
            @PathVariable String uuid,@RequestBody @jakarta.validation.Valid PatchMessageRequest request) {
        return ok(service.patch(MessageTenantType.DOCTOR,tenantId(),uuid,request,email()));
    }
    @PostMapping("/messages/{uuid}/{action}") public ResponseEntity<SuccessResponse<MessageModel>> action(
            @PathVariable String uuid,@PathVariable String action) {
        return ok(service.action(MessageTenantType.DOCTOR,tenantId(),uuid,action,email()));
    }
    @PostMapping("/pause-all") public ResponseEntity<SuccessResponse<SettingsModel>> pause() {
        return ok(service.setPause(MessageTenantType.DOCTOR,tenantId(),true,email()));
    }
    @PostMapping("/resume-all") public ResponseEntity<SuccessResponse<SettingsModel>> resume() {
        return ok(service.setPause(MessageTenantType.DOCTOR,tenantId(),false,email()));
    }
    @GetMapping("/automations") public ResponseEntity<SuccessResponse<java.util.List<AutomationItem>>> automations() {
        return ok(service.getAutomations(MessageTenantType.DOCTOR,tenantId()).automations());
    }
    @PutMapping("/automations") public ResponseEntity<SuccessResponse<java.util.List<AutomationItem>>> automations(
            @RequestBody @jakarta.validation.Valid java.util.List<@jakarta.validation.Valid AutomationItem> request) {
        java.util.Map<String,Boolean> enabled=request.stream().collect(java.util.stream.Collectors.toMap(
                AutomationItem::id,AutomationItem::enabled,(a,b)->b));
        return ok(service.setAutomations(MessageTenantType.DOCTOR,tenantId(),enabled,email()).automations());
    }
    @GetMapping("/settings") public ResponseEntity<SuccessResponse<SettingsModel>> settings() {
        return ok(service.getSettings(MessageTenantType.DOCTOR,tenantId()));
    }
    @PutMapping("/settings") public ResponseEntity<SuccessResponse<SettingsModel>> settings(
            @RequestBody @jakarta.validation.Valid SettingsRequest request) {
        return ok(service.setPause(MessageTenantType.DOCTOR,tenantId(),request.effectivePaused(),email()));
    }
    @GetMapping("/credits") public ResponseEntity<SuccessResponse<CreditsModel>> credits() {
        return ok(service.credits(MessageTenantType.DOCTOR,tenantId()));
    }
    @GetMapping("/audit") public ResponseEntity<SuccessResponse<java.util.List<AuditModel>>> audit() {
        return ok(service.auditEvents(MessageTenantType.DOCTOR,tenantId()));
    }
    @PostMapping("/credits/purchase") public ResponseEntity<SuccessResponse<PurchaseModel>> purchase(
            @RequestBody @jakarta.validation.Valid PurchaseRequest request) {
        return ok(service.createPurchase(MessageTenantType.DOCTOR,tenantId(),request.effectiveChannel(),request.bundleId()),HttpStatus.CREATED);
    }
    @PostMapping("/credits/purchase/verify") public ResponseEntity<SuccessResponse<PurchaseModel>> verify(
            @RequestBody @jakarta.validation.Valid VerifyPurchaseRequest request) {
        return ok(service.verifyPurchase(MessageTenantType.DOCTOR,tenantId(),request,email()));
    }

    private Long tenantId() {
        DoctorProfile profile=doctorProfiles.findByUserId(userDao.userByEmail(email()).getId());
        if(profile==null) throw new CustomException("Doctor profile not found",HttpStatus.NOT_FOUND);
        return profile.getId();
    }
    private String email(){ return SecurityContextHolder.getContext().getAuthentication().getName(); }
    private <T> ResponseEntity<SuccessResponse<T>> ok(T data){ return ok(data,HttpStatus.OK); }
    private <T> ResponseEntity<SuccessResponse<T>> ok(T data,HttpStatus status){
        return response.buildSuccessResponse(data,ResponseMessage.SUCCESS,status);
    }
}
