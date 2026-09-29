package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.dto.ClinicReportsDto.ReportModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPatientPetRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.visit.repository.VisitRepository;

@ExtendWith(MockitoExtension.class)
class ClinicReportsServiceImplTest {

    @Mock
    private ClinicService clinicService;
    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ConsultationInvoiceRepository invoiceRepository;
    @Mock
    private ClinicPatientPetRepository patientRepository;
    @Mock
    private ClinicDoctorRepository clinicDoctorRepository;
        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ClinicReportsServiceImpl reportsService;

    @Test
    void reportsAggregatesRealInvoiceDataForRequestedPeriod() {
        Clinic clinic = Clinic.builder().uuid("clinic-1").build();
        clinic.setId(7L);
        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .status(ConsultationInvoiceStatus.PAID)
                .amount(new BigDecimal("1500.00"))
                .paidAmount(new BigDecimal("1200.00"))
                .currency("INR")
                .consultationDate(LocalDate.of(2026, 9, 12))
                .lineItems("[{\"description\":\"Consultation\",\"quantity\":1,\"total\":1500}]")
                .build();
        invoice.setCreatedAt(LocalDateTime.of(2026, 9, 12, 10, 0));

        when(clinicRepository.findByUuid("clinic-1")).thenReturn(clinic);
        when(visitRepository.countForClinicReport(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(visitRepository.countByDoctorForClinicReport(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(bookingRepository.countByClinic_IdAndIsActiveTrueAndSlotStartBetween(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(invoiceRepository.findForClinicReport(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(invoice));
        when(patientRepository.countByClinic_IdAndIsActiveTrue(7L)).thenReturn(4L);
        when(patientRepository.countByClinic_IdAndIsActiveTrueAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1L);
        when(clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(7L)).thenReturn(List.of());

        ReportModel report = reportsService.reports("clinic-1", "owner@example.com",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(new BigDecimal("1500.00"), report.revenue().billed());
        assertEquals(new BigDecimal("1200.00"), report.revenue().paid());
        assertEquals(4L, report.patients().total());
        assertEquals(1L, report.patients().newPatients());
        assertEquals("2026-09", report.monthlyRevenue().get(0).month());
        assertEquals("Consultation", report.services().get(0).service());
    }

    @Test
    void reportsRejectsReversedPeriod() {
        Clinic clinic = Clinic.builder().uuid("clinic-1").build();
        clinic.setId(7L);
        when(clinicRepository.findByUuid("clinic-1")).thenReturn(clinic);

        org.junit.jupiter.api.Assertions.assertThrows(CustomException.class, () -> reportsService.reports("clinic-1", "owner@example.com",
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1)));
    }

        @Test
        void reportsRejectsPeriodsLongerThanOneYear() {
                Clinic clinic = Clinic.builder().uuid("clinic-1").build();
                clinic.setId(7L);
                when(clinicRepository.findByUuid("clinic-1")).thenReturn(clinic);

                org.junit.jupiter.api.Assertions.assertThrows(CustomException.class, () -> reportsService.reports("clinic-1",
                                "owner@example.com", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
        }
}
