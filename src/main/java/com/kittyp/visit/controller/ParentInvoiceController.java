package com.kittyp.visit.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.doctor.dto.OwnerInvoiceModel;
import com.kittyp.doctor.service.TreatmentInvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ParentInvoiceController {

    private final TreatmentInvoiceService treatmentInvoiceService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.PET_INVOICES)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PaginationModel<OwnerInvoiceModel>>> petInvoices(
            @PathVariable String uuid,
            @RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(defaultValue = KeyConstant.PAGE_SIZE) Integer pageSize) {
        return responseBuilder.buildSuccessResponse(
                treatmentInvoiceService.pageForPetOwner(uuid, email(), pageNumber, pageSize),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.PET_INVOICE_PDF)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<Map<String, String>>> petInvoicePdf(
            @PathVariable String uuid,
            @PathVariable String invoiceUuid) {
        String url = treatmentInvoiceService.pdfUrlForPetOwner(uuid, invoiceUuid, email());
        return responseBuilder.buildSuccessResponse(Map.of("url", url), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
