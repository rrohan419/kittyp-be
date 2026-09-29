package com.kittyp.clinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ClinicReportsDto {

    private ClinicReportsDto() {
    }

    public record ReportModel(
            ReportPeriod period,
            RevenueModel revenue,
            VisitMetrics visits,
            PatientMetrics patients,
            List<MonthlyRevenue> monthlyRevenue,
            List<ServiceBreakdown> services,
            List<DoctorPerformance> doctorPerformance) {
    }

    public record ReportPeriod(LocalDate from, LocalDate to) {
    }

    public record RevenueModel(BigDecimal billed, BigDecimal paid, String currency) {
    }

    public record VisitMetrics(long total, long completed, long cancelled, long noShows, long bookings) {
    }

    public record PatientMetrics(long total, long newPatients) {
    }

    public record MonthlyRevenue(String month, BigDecimal billed, BigDecimal paid) {
    }

    public record ServiceBreakdown(String service, long count, BigDecimal billed) {
    }

    public record DoctorPerformance(String doctorUuid, String doctorName, long visits, long completedVisits,
            BigDecimal billed) {
    }
}
