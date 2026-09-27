package com.kittyp.clinic.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.dto.ClinicInventoryDtos.WeeklyReportModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClinicInventoryWeeklyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClinicInventoryWeeklyReportScheduler.class);

    private final ClinicRepository clinicRepository;
    private final ClinicInventoryService inventoryService;
    private final ClinicInventoryAlertService alertService;

    @Value("${kittyp.inventory.weekly-enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${kittyp.inventory.weekly-cron:0 0 8 * * MON}")
    @Transactional
    public void runWeeklyReports() {
        if (!enabled) {
            return;
        }
        List<Clinic> clinics = clinicRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .toList();
        log.info("Inventory weekly report: processing {} clinics", clinics.size());
        for (Clinic clinic : clinics) {
            try {
                alertService.evaluateClinic(clinic);
                WeeklyReportModel report = inventoryService.buildWeeklyReport(clinic);
                String msg = "Your weekly inventory report is ready. Low stock: "
                        + report.lowStock().size()
                        + ", Expiring soon: "
                        + report.expiringSoon().size()
                        + ", Expired: "
                        + report.expired().size()
                        + ".";
                alertService.notifyWeeklyReport(clinic, msg);
            } catch (Exception e) {
                log.warn("Weekly inventory report failed for clinic {}: {}", clinic.getUuid(), e.getMessage());
            }
        }
    }
}
