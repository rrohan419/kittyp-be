package com.kittyp.product.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.product.dao.StockReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationCleanupService {

    private final StockReservationRepository stockReservationRepository;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredReservations() {
        log.info("Starting cleanup of expired stock reservations");
        try {
            var expiredReservations = stockReservationRepository.findByExpirationTimeBefore(LocalDateTime.now());
            if (!expiredReservations.isEmpty()) {
                stockReservationRepository.deleteAll(expiredReservations);
                log.info("Cleaned up {} expired stock reservations", expiredReservations.size());
            }
        } catch (Exception e) {
            log.error("Error during stock reservation cleanup", e);
        }
    }
} 