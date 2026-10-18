package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.service.ClinicInventoryConsumptionService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.service.S3StorageService;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.payment.util.PdfGenerator;

@ExtendWith(MockitoExtension.class)
class TreatmentInvoiceInventoryIssueTest {

    @Mock
    private ConsultationInvoiceRepository invoiceRepository;
    @Mock
    private PdfGenerator pdfGenerator;
    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private ClinicInventoryConsumptionService inventoryConsumptionService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private TreatmentInvoiceService service;

    @Test
    void draftIssueConsumesStockOnceAndReissueDoesNot() {
        ConsultationInvoice invoice = draft();
        when(pdfGenerator.generateTreatmentInvoicePdf(any())).thenReturn(new byte[] { 1 });
        when(s3StorageService.uploadTreatmentInvoice(any(), any())).thenReturn("invoices/inv.pdf");
        when(invoiceRepository.save(any(ConsultationInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generateAndAttachPdf(invoice);
        service.generateAndAttachPdf(invoice);

        verify(inventoryConsumptionService, times(1)).consumeForIssue(invoice);
        assertEquals(ConsultationInvoiceStatus.ISSUED, invoice.getStatus());
    }

    @Test
    void shortStockBlocksIssueWithoutPdf() {
        ConsultationInvoice invoice = draft();
        doThrow(new CustomException("Only 1 Amoxicillin on hand. Confirm billing 4 to continue.",
                HttpStatus.CONFLICT)).when(inventoryConsumptionService).consumeForIssue(invoice);

        CustomException ex = assertThrows(CustomException.class, () -> service.generateAndAttachPdf(invoice));

        assertEquals(409, ex.getHttpStatus().value());
        assertEquals(ConsultationInvoiceStatus.DRAFT, invoice.getStatus());
        verify(pdfGenerator, never()).generateTreatmentInvoicePdf(any());
        verify(invoiceRepository, never()).save(any());
    }

    private static ConsultationInvoice draft() {
        Clinic clinic = Clinic.builder().uuid("clinic-1").name("Main").build();
        clinic.setId(10L);
        return ConsultationInvoice.builder()
                .uuid("inv-1")
                .invoiceNumber("INV-1")
                .clinic(clinic)
                .lineItems("[]")
                .status(ConsultationInvoiceStatus.DRAFT)
                .build();
    }
}
