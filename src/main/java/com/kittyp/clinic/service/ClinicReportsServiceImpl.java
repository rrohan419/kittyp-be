package com.kittyp.clinic.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.summingLong;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.clinic.dto.ClinicReportsDto.DoctorPerformance;
import com.kittyp.clinic.dto.ClinicReportsDto.MonthlyRevenue;
import com.kittyp.clinic.dto.ClinicReportsDto.PatientMetrics;
import com.kittyp.clinic.dto.ClinicReportsDto.ReportModel;
import com.kittyp.clinic.dto.ClinicReportsDto.ReportPeriod;
import com.kittyp.clinic.dto.ClinicReportsDto.RevenueModel;
import com.kittyp.clinic.dto.ClinicReportsDto.ServiceBreakdown;
import com.kittyp.clinic.dto.ClinicReportsDto.VisitMetrics;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPatientPetRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.user.entity.User;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.repository.VisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicReportsServiceImpl implements ClinicReportsService {

    private static final long MAX_REPORT_PERIOD_DAYS = 366;

    private final ClinicService clinicService;
    private final ClinicRepository clinicRepository;
    private final VisitRepository visitRepository;
    private final BookingRepository bookingRepository;
    private final ConsultationInvoiceRepository invoiceRepository;
    private final ClinicPatientPetRepository patientRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ReportModel reports(String clinicUuid, String email, LocalDate from, LocalDate to) {
        clinicService.get(clinicUuid, email);
        Clinic clinic = clinicRepository.findByUuid(clinicUuid);
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate resolvedTo = to != null ? to : resolvedFrom.withDayOfMonth(resolvedFrom.lengthOfMonth());
        if (resolvedTo.isBefore(resolvedFrom)) {
            throw new CustomException("Report end date must not be before start date", HttpStatus.BAD_REQUEST);
        }
        if (ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) >= MAX_REPORT_PERIOD_DAYS) {
            throw new CustomException("Report period must not exceed 366 days", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime fromDateTime = resolvedFrom.atStartOfDay();
        LocalDateTime toDateTime = resolvedTo.plusDays(1).atStartOfDay().minusNanos(1);
        List<VisitRepository.ReportStatusCount> visitCounts = visitRepository.countForClinicReport(
            clinic.getId(), fromDateTime, toDateTime);
        List<VisitRepository.ReportDoctorVisitCount> doctorVisitCounts = visitRepository.countByDoctorForClinicReport(
            clinic.getId(), fromDateTime, toDateTime);
        long bookings = bookingRepository.countByClinic_IdAndIsActiveTrueAndSlotStartBetween(
            clinic.getId(), fromDateTime, toDateTime);
        List<ConsultationInvoice> invoices = invoiceRepository.findForClinicReport(clinic.getId(), resolvedFrom,
            resolvedTo, fromDateTime, toDateTime);

        return new ReportModel(
                new ReportPeriod(resolvedFrom, resolvedTo),
                revenue(invoices),
                visitMetrics(visitCounts, bookings),
                new PatientMetrics(patientRepository.countByClinic_IdAndIsActiveTrue(clinic.getId()),
                        patientRepository.countByClinic_IdAndIsActiveTrueAndCreatedAtBetween(
                                clinic.getId(), fromDateTime, toDateTime)),
                monthlyRevenue(invoices),
                services(invoices),
                doctorPerformance(clinic.getId(), doctorVisitCounts, invoices));
    }

    private RevenueModel revenue(List<ConsultationInvoice> invoices) {
        BigDecimal billed = invoices.stream().filter(this::isBillable)
                .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = invoices.stream().filter(this::isBillable)
                .map(invoice -> Optional.ofNullable(invoice.getPaidAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = invoices.stream().map(ConsultationInvoice::getCurrency).filter(Objects::nonNull).findFirst()
                .orElse("INR");
        return new RevenueModel(billed, paid, currency);
    }

    private VisitMetrics visitMetrics(List<VisitRepository.ReportStatusCount> visitCounts, long bookings) {
        Map<VisitStatus, Long> counts = visitCounts.stream().collect(toMap(
                VisitRepository.ReportStatusCount::getStatus, VisitRepository.ReportStatusCount::getVisitCount));
        return new VisitMetrics(visitCounts.stream().mapToLong(VisitRepository.ReportStatusCount::getVisitCount).sum(),
                counts.getOrDefault(VisitStatus.COMPLETED, 0L), counts.getOrDefault(VisitStatus.CANCELLED, 0L),
                counts.getOrDefault(VisitStatus.NO_SHOW, 0L), bookings);
    }

    private List<MonthlyRevenue> monthlyRevenue(List<ConsultationInvoice> invoices) {
        Map<YearMonth, List<ConsultationInvoice>> grouped = invoices.stream().filter(this::isBillable)
                .collect(groupingBy(invoice -> YearMonth.from(effectiveInvoiceDate(invoice).orElseThrow()),
                        LinkedHashMap::new, toList()));
        return grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> new MonthlyRevenue(
                entry.getKey().toString(), entry.getValue().stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                entry.getValue().stream().map(invoice -> Optional.ofNullable(invoice.getPaidAmount()).orElse(BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))).toList();
    }

    private List<ServiceBreakdown> services(List<ConsultationInvoice> invoices) {
        Map<String, ServiceAccumulator> grouped = new LinkedHashMap<>();
        for (ConsultationInvoice invoice : invoices) {
            if (!isBillable(invoice)) {
                continue;
            }
            try {
                JsonNode items = objectMapper.readTree(invoice.getLineItems());
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        String service = item.path("description").asText("Other");
                        BigDecimal itemTotal = decimal(item, "total").orElse(decimal(item, "unitPrice")
                                .orElse(BigDecimal.ZERO).multiply(BigDecimal.valueOf(item.path("quantity").asDouble(1))));
                        grouped.computeIfAbsent(service, ignored -> new ServiceAccumulator()).add(itemTotal);
                    }
                }
            } catch (Exception ignored) {
                grouped.computeIfAbsent("Other", ignoredKey -> new ServiceAccumulator()).add(amount(invoice));
            }
        }
        return grouped.entrySet().stream().map(entry -> new ServiceBreakdown(entry.getKey(), entry.getValue().count,
                entry.getValue().billed)).sorted(Comparator.comparing(ServiceBreakdown::billed).reversed()).toList();
    }

    private List<DoctorPerformance> doctorPerformance(Long clinicId,
            List<VisitRepository.ReportDoctorVisitCount> visitCounts,
            List<ConsultationInvoice> invoices) {
        Map<Long, Map<VisitStatus, Long>> visitsByDoctor = visitCounts.stream().collect(groupingBy(
                VisitRepository.ReportDoctorVisitCount::getDoctorId,
                groupingBy(VisitRepository.ReportDoctorVisitCount::getStatus, summingLong(
                        VisitRepository.ReportDoctorVisitCount::getVisitCount))));
        Map<Long, BigDecimal> revenueByDoctor = new LinkedHashMap<>();
        for (ConsultationInvoice invoice : invoices) {
            if (isBillable(invoice) && invoice.getDoctor() != null) {
                revenueByDoctor.merge(invoice.getDoctor().getId(), amount(invoice), BigDecimal::add);
            }
        }
        List<DoctorPerformance> result = new ArrayList<>();
        for (ClinicDoctor affiliation : clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinicId)) {
            if (affiliation.getDoctor() == null) {
                continue;
            }
            User user = affiliation.getDoctor().getUser();
            Map<VisitStatus, Long> doctorVisits = visitsByDoctor.getOrDefault(affiliation.getDoctor().getId(), Map.of());
            long totalVisits = doctorVisits.values().stream().mapToLong(Long::longValue).sum();
            String name = user == null ? "Unknown doctor" : ((user.getFirstName() == null ? "" : user.getFirstName())
                    + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
            result.add(new DoctorPerformance(affiliation.getDoctor().getUuid(), name.isBlank() ? "Unknown doctor" : name,
                    totalVisits, doctorVisits.getOrDefault(VisitStatus.COMPLETED, 0L),
                    user == null ? BigDecimal.ZERO : revenueByDoctor.getOrDefault(user.getId(), BigDecimal.ZERO)));
        }
        return result;
    }

    private boolean isBillable(ConsultationInvoice invoice) {
        return invoice.getStatus() != null && invoice.getStatus() != ConsultationInvoiceStatus.DRAFT;
    }

    private BigDecimal amount(ConsultationInvoice invoice) {
        return Optional.ofNullable(invoice.getAmount()).orElse(BigDecimal.ZERO);
    }

    private Optional<LocalDate> effectiveInvoiceDate(ConsultationInvoice invoice) {
        return Optional.ofNullable(invoice.getConsultationDate())
                .or(() -> Optional.ofNullable(invoice.getCreatedAt()).map(LocalDateTime::toLocalDate));
    }

    private Optional<BigDecimal> decimal(JsonNode node, String field) {
        return node.hasNonNull(field) ? Optional.of(BigDecimal.valueOf(node.path(field).asDouble())) : Optional.empty();
    }

    private static final class ServiceAccumulator {
        private long count;
        private BigDecimal billed = BigDecimal.ZERO;

        private void add(BigDecimal value) {
            count++;
            billed = billed.add(value);
        }
    }
}
