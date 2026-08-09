package com.kittyp.doctor.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dto.CreateConsultationInvoiceDto;
import com.kittyp.doctor.dto.UpdateConsultationInvoiceStatusDto;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ConsultationInvoiceController {

    private final ApiResponse<?> responseBuilder;
    private final ConsultationInvoiceRepository consultationInvoiceRepository;
    private final UserRepository userRepository;
    private final TreatmentInvoiceService treatmentInvoiceService;

    @PostMapping(ApiUrl.CONSULTATION_INVOICE_BASE_URL)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> createInvoice(
            @Valid @RequestBody CreateConsultationInvoiceDto request) {
        ConsultationInvoice invoice = treatmentInvoiceService.createAndOptionallySendWhatsApp(currentUser(), request);
        return responseBuilder.buildSuccessResponse(invoice, ResponseMessage.SUCCESS, HttpStatus.CREATED);
    }

    @GetMapping(ApiUrl.CONSULTATION_INVOICE_MINE)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<List<ConsultationInvoice>>> myInvoices() {
        User doctor = currentUser();
        List<ConsultationInvoice> mine = consultationInvoiceRepository
                .findAllByDoctor_IdOrderByCreatedAtDesc(doctor.getId())
                .stream()
                .filter(inv -> isDoctorPersonalInvoice(inv, doctor))
                .toList();
        return responseBuilder.buildSuccessResponse(mine, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CONSULTATION_INVOICE_BY_UUID)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> getInvoice(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(requireDoctorOperableInvoice(uuid), ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PatchMapping(ApiUrl.CONSULTATION_INVOICE_STATUS)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> updateStatus(
            @PathVariable String uuid, @Valid @RequestBody UpdateConsultationInvoiceStatusDto request) {
        ConsultationInvoice invoice = requireDoctorOperableInvoice(uuid);
        invoice.setStatus(request.getStatus());
        return responseBuilder.buildSuccessResponse(consultationInvoiceRepository.save(invoice),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CONSULTATION_INVOICE_GENERATE_PDF)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> generatePdf(@PathVariable String uuid) {
        ConsultationInvoice invoice = treatmentInvoiceService.generateAndAttachPdf(requireDoctorOperableInvoice(uuid));
        return responseBuilder.buildSuccessResponse(invoice, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CONSULTATION_INVOICE_SEND_WHATSAPP)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ConsultationInvoice>> sendWhatsApp(@PathVariable String uuid) {
        ConsultationInvoice invoice = treatmentInvoiceService.sendInvoiceWhatsApp(
                requireDoctorOperableInvoice(uuid), null);
        return responseBuilder.buildSuccessResponse(invoice, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CONSULTATION_INVOICE_PDF)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<Map<String, String>>> getPdfUrl(@PathVariable String uuid) {
        ConsultationInvoice invoice = requireDoctorOperableInvoice(uuid);
        String url = treatmentInvoiceService.getPresignedPdfUrl(invoice);
        return responseBuilder.buildSuccessResponse(Map.of("url", url), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Doctor may only read/mutate/send invoices for personal practice (no clinic, or clinic they own).
     * Clinic-branch invoices must go through clinic APIs (prevents wrong WhatsApp sender + status IDOR).
     */
    private ConsultationInvoice requireDoctorOperableInvoice(String uuid) {
        User doctor = currentUser();
        ConsultationInvoice invoice = consultationInvoiceRepository.findByUuidAndDoctor_Id(uuid, doctor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation invoice", "uuid", uuid));
        if (!isDoctorPersonalInvoice(invoice, doctor)) {
            throw new AccessDeniedException("Clinic invoices must be managed from the clinic portal.");
        }
        return invoice;
    }

    private static boolean isDoctorPersonalInvoice(ConsultationInvoice invoice, User doctor) {
        Clinic clinic = invoice.getClinic();
        if (clinic == null) {
            return true;
        }
        return clinic.getOwner() != null && clinic.getOwner().getId().equals(doctor.getId());
    }
}
