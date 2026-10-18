package com.kittyp.clinic.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicInventoryReservationReleaseService {

    private final ClinicInventoryConsumptionService consumptionService;

    @Scheduled(fixedDelay = 30000)
    public void releaseExpiredHolds() {
        try {
            consumptionService.releaseExpiredReservations();
        } catch (Exception e) {
            log.warn("Could not release expired inventory holds: {}", e.getMessage());
        }
    }
}
