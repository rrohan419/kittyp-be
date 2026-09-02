package com.kittyp.clinic.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dto.CreateConsultationInvoiceDto;
import com.kittyp.doctor.dto.CreateInvoiceResultDto;
import com.kittyp.doctor.dto.MarkInvoicePaidDto;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.notification.service.WhatsAppCredentialsVerifier;
import com.kittyp.notification.service.WhatsAppSettingsSupport;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicInvoiceController {

    private static final String CLINIC_BILLING = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF;

    private final ApiResponse<?> responseBuilder;
    private final ClinicService clinicService;
    private final ClinicRepository clinicRepository;
    private final TreatmentInvoiceService treatmentInvoiceService;
    private final UserRepository userRepository;
    private final WhatsAppCredentialsVerifier whatsAppCredentialsVerifier;

    @GetMapping(ApiUrl.CLINIC_INVOICES)
    @PreAuthorize(CLINIC_BILLING)
    public ResponseEntity<SuccessResponse<PaginationModel<ConsultationInvoice>>> list(
            @PathVariable String uuid,
            @RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(defaultValue = KeyConstant.PAGE_SIZE) Integer pageSize) {
        Clinic clinic = requireAccessibleClinic(uuid);
        return responseBuilder.buildSuccessResponse(
                treatmentInvoiceService.pageForClinic(clinic, pageNumber, pageSize),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CLINIC_INVOICES)
    @PreAuthorize(CLINIC_BILLING)
    public ResponseEntity<SuccessResponse<CreateInvoiceResultDto>> create(
            @PathVariable String uuid, @Valid @RequestBody CreateConsultationInvoiceDto request) {
        Clinic clinic = requireAccessibleClinic(uuid);
        CreateInvoiceResultDto result = treatmentInvoiceService.createForClinicAndOptionallySend(
                clinic, currentUser(), request);
        return responseBuilder.buildSuccessResponse(result, ResponseMessage.SUCCESS, HttpStatus.CREATED);
    }

    @PostMapping(ApiUrl.CLINIC_INVOICE_MARK_PAID)
    @PreAuthorize(CLINIC_BILLING)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> markPaid(
            @PathVariable String uuid, @PathVariable String invoiceUuid,
            @Valid @RequestBody MarkInvoicePaidDto request) {
        Clinic clinic = requireAccessibleClinic(uuid);
        ConsultationInvoice invoice = treatmentInvoiceService.requireClinicInvoice(clinic, invoiceUuid);
        invoice = treatmentInvoiceService.markPaid(invoice, request.getPaymentMode(), request.getTransactionId());
        invoice = treatmentInvoiceService.refreshPdfQuietly(invoice);
        return responseBuilder.buildSuccessResponse(invoice, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CLINIC_INVOICE_GENERATE_PDF)
    @PreAuthorize(CLINIC_BILLING)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> generatePdf(
            @PathVariable String uuid, @PathVariable String invoiceUuid) {
        Clinic clinic = requireAccessibleClinic(uuid);
        ConsultationInvoice invoice = treatmentInvoiceService.requireClinicInvoice(clinic, invoiceUuid);
        return responseBuilder.buildSuccessResponse(
                treatmentInvoiceService.generateAndAttachPdf(invoice), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVOICE_PDF)
    @PreAuthorize(CLINIC_BILLING)
    public ResponseEntity<SuccessResponse<Map<String, String>>> pdfUrl(
            @PathVariable String uuid, @PathVariable String invoiceUuid) {
        Clinic clinic = requireAccessibleClinic(uuid);
        ConsultationInvoice invoice = treatmentInvoiceService.requireClinicInvoice(clinic, invoiceUuid);
        String url = treatmentInvoiceService.getPresignedPdfUrl(invoice);
        return responseBuilder.buildSuccessResponse(Map.of("url", url), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CLINIC_INVOICE_SEND_WHATSAPP)
    @PreAuthorize(CLINIC_BILLING)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> sendWhatsApp(
            @PathVariable String uuid, @PathVariable String invoiceUuid) {
        Clinic clinic = requireAccessibleClinic(uuid);
        ConsultationInvoice invoice = treatmentInvoiceService.requireClinicInvoice(clinic, invoiceUuid);
        invoice = treatmentInvoiceService.sendInvoiceWhatsApp(
                invoice, null, treatmentInvoiceService.clinicSender(clinic));
        return responseBuilder.buildSuccessResponse(invoice, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_WHATSAPP_SETTINGS)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getWhatsApp(@PathVariable String uuid) {
        Clinic clinic = requireManagedClinic(uuid);
        return responseBuilder.buildSuccessResponse(
                WhatsAppSettingsSupport.publicView(
                        clinic.getWhatsappPhoneNumberId(),
                        clinic.getWhatsappBusinessAccountId(),
                        clinic.getWhatsappToken()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PutMapping(ApiUrl.CLINIC_WHATSAPP_SETTINGS)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<Map<String, Object>>> updateWhatsApp(
            @PathVariable String uuid, @Valid @RequestBody WhatsAppSettingsRequest request) {
        Clinic clinic = requireManagedClinic(uuid);
        String phoneNumberId = request.getPhoneNumberId().trim();
        String businessAccountId = request.getBusinessAccountId().trim();
        String tokenToStore;
        if (StringUtils.hasText(request.getToken())) {
            tokenToStore = request.getToken().trim();
        } else if (StringUtils.hasText(clinic.getWhatsappToken())) {
            tokenToStore = clinic.getWhatsappToken();
        } else {
            throw new CustomException("token is required for first-time WhatsApp setup", HttpStatus.BAD_REQUEST);
        }
        whatsAppCredentialsVerifier.verifyOrThrow(tokenToStore, phoneNumberId, businessAccountId);
        clinic.setWhatsappPhoneNumberId(phoneNumberId);
        clinic.setWhatsappBusinessAccountId(businessAccountId);
        clinic.setWhatsappToken(tokenToStore);
        clinicRepository.save(clinic);
        return responseBuilder.buildSuccessResponse(
                WhatsAppSettingsSupport.publicView(
                        clinic.getWhatsappPhoneNumberId(),
                        clinic.getWhatsappBusinessAccountId(),
                        clinic.getWhatsappToken()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    private Clinic requireAccessibleClinic(String clinicUuid) {
        // Ensures caller is affiliated / staff for this clinic
        clinicService.get(clinicUuid, email());
        Clinic clinic = clinicRepository.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("Clinic", "uuid", clinicUuid);
        }
        return clinic;
    }

    /** Owner or staff/admin of this clinic — not affiliated-only doctors. */
    private Clinic requireManagedClinic(String clinicUuid) {
        clinicService.requireClinicManager(clinicUuid, email());
        Clinic clinic = clinicRepository.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("Clinic", "uuid", clinicUuid);
        }
        return clinic;
    }

    private User currentUser() {
        return userRepository.findByEmail(email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email()));
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Data
    public static class WhatsAppSettingsRequest {
        @NotBlank
        @jakarta.validation.constraints.Size(max = 64)
        private String phoneNumberId;
        @NotBlank
        @jakarta.validation.constraints.Size(max = 64)
        private String businessAccountId;
        /** Optional on update if already set — omit to keep existing token. */
        @jakarta.validation.constraints.Size(max = 2048)
        private String token;
    }
}
