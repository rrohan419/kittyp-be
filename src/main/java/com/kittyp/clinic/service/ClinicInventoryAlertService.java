package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryAlertState;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryLot;
import com.kittyp.clinic.enums.InventoryAlertType;
import com.kittyp.clinic.enums.InventoryExpiryStatus;
import com.kittyp.clinic.enums.InventoryStockStatus;
import com.kittyp.clinic.repository.ClinicInventoryAlertStateRepository;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryLotRepository;
import com.kittyp.notification.FcmPushNotificationService;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;
import com.kittyp.user.repository.UserFcmTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicInventoryAlertService {

    private static final Logger log = LoggerFactory.getLogger(ClinicInventoryAlertService.class);

    private final ClinicInventoryAlertStateRepository alertStateRepository;
    private final ClinicInventoryItemRepository itemRepository;
    private final ClinicInventoryLotRepository lotRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final FcmPushNotificationService fcmPushNotificationService;
    private final UserFcmTokenRepository userFcmTokenRepository;

    @Transactional
    public void evaluateItem(Clinic clinic, ClinicInventoryItem item) {
        if (clinic == null || item == null || !Boolean.TRUE.equals(item.getTrackStock())) {
            return;
        }
        LocalDate today = LocalDate.now();
        InventoryStockStatus stock = InventoryStatusSupport.stockStatus(item);
        upsertStockAlert(clinic, item, stock);

        List<ClinicInventoryLot> lots =
                lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId());
        InventoryExpiryStatus expiry = InventoryStatusSupport.itemExpiryStatus(lots, clinic, today);
        upsertExpiryAlert(clinic, item, expiry, lots, today);
    }

    @Transactional
    public void evaluateClinic(Clinic clinic) {
        if (clinic == null) {
            return;
        }
        for (ClinicInventoryItem item :
                itemRepository.findByClinic_IdAndIsActiveTrueOrderByNameAsc(clinic.getId())) {
            evaluateItem(clinic, item);
        }
    }

    @Transactional
    public void notifyWeeklyReport(Clinic clinic, String summaryMessage) {
        String weekKey = LocalDate.now().toString();
        var existing = alertStateRepository.findByClinic_IdAndAlertTypeAndInventoryItemIsNullAndStateKey(
                clinic.getId(), InventoryAlertType.WEEKLY_SUMMARY, weekKey);
        if (existing.isPresent() && existing.get().getResolvedAt() == null
                && existing.get().getLastNotifiedAt() != null) {
            return;
        }
        ClinicInventoryAlertState state = existing.orElseGet(() -> ClinicInventoryAlertState.builder()
                .clinic(clinic)
                .alertType(InventoryAlertType.WEEKLY_SUMMARY)
                .stateKey(weekKey)
                .build());
        state.setIsActive(true);
        state.setMessage(summaryMessage);
        state.setResolvedAt(null);
        state.setLastNotifiedAt(LocalDateTime.now());
        alertStateRepository.save(state);
        notifyClinicOwner(clinic, NotificationType.INVENTORY_WEEKLY_REPORT, summaryMessage);
    }

    private void upsertStockAlert(Clinic clinic, ClinicInventoryItem item, InventoryStockStatus stock) {
        if (stock == InventoryStockStatus.IN_STOCK) {
            resolve(clinic, item, InventoryAlertType.LOW_STOCK, "active");
            resolve(clinic, item, InventoryAlertType.OUT_OF_STOCK, "active");
            return;
        }
        if (stock == InventoryStockStatus.OUT_OF_STOCK) {
            resolve(clinic, item, InventoryAlertType.LOW_STOCK, "active");
            fireOnce(
                    clinic,
                    item,
                    InventoryAlertType.OUT_OF_STOCK,
                    "active",
                    item.getName() + " is out of stock.",
                    NotificationType.INVENTORY_OUT_OF_STOCK);
            return;
        }
        resolve(clinic, item, InventoryAlertType.OUT_OF_STOCK, "active");
        String msg = item.getName() + " stock is low. "
                + strip(item.getStock()) + " units remaining. Minimum required: "
                + (item.getMinStock() == null ? 0 : item.getMinStock()) + ".";
        fireOnce(clinic, item, InventoryAlertType.LOW_STOCK, "active", msg, NotificationType.INVENTORY_LOW_STOCK);
    }

    private void upsertExpiryAlert(
            Clinic clinic,
            ClinicInventoryItem item,
            InventoryExpiryStatus expiry,
            List<ClinicInventoryLot> lots,
            LocalDate today) {
        if (expiry == InventoryExpiryStatus.NORMAL) {
            resolve(clinic, item, InventoryAlertType.EXPIRING_SOON, "active");
            resolve(clinic, item, InventoryAlertType.EXPIRED, "active");
            return;
        }
        if (expiry == InventoryExpiryStatus.EXPIRED) {
            resolve(clinic, item, InventoryAlertType.EXPIRING_SOON, "active");
            fireOnce(
                    clinic,
                    item,
                    InventoryAlertType.EXPIRED,
                    "active",
                    item.getName() + " has expired.",
                    NotificationType.INVENTORY_EXPIRED);
            return;
        }
        resolve(clinic, item, InventoryAlertType.EXPIRED, "active");
        LocalDate earliest = InventoryStatusSupport.earliestExpiry(lots);
        long days = earliest == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(today, earliest);
        fireOnce(
                clinic,
                item,
                InventoryAlertType.EXPIRING_SOON,
                "active",
                item.getName() + " will expire in " + days + " days.",
                NotificationType.INVENTORY_EXPIRING_SOON);
    }

    private void fireOnce(
            Clinic clinic,
            ClinicInventoryItem item,
            InventoryAlertType type,
            String stateKey,
            String message,
            NotificationType notificationType) {
        var existing = alertStateRepository.findByClinic_IdAndAlertTypeAndInventoryItem_IdAndStateKey(
                clinic.getId(), type, item.getId(), stateKey);
        if (existing.isPresent()
                && existing.get().getResolvedAt() == null
                && existing.get().getLastNotifiedAt() != null) {
            return;
        }
        ClinicInventoryAlertState state = existing.orElseGet(() -> ClinicInventoryAlertState.builder()
                .clinic(clinic)
                .inventoryItem(item)
                .alertType(type)
                .stateKey(stateKey)
                .build());
        state.setIsActive(true);
        state.setMessage(message);
        state.setResolvedAt(null);
        state.setLastNotifiedAt(LocalDateTime.now());
        alertStateRepository.save(state);
        notifyClinicOwner(clinic, notificationType, message);
    }

    private void resolve(Clinic clinic, ClinicInventoryItem item, InventoryAlertType type, String stateKey) {
        alertStateRepository
                .findByClinic_IdAndAlertTypeAndInventoryItem_IdAndStateKey(
                        clinic.getId(), type, item.getId(), stateKey)
                .ifPresent(state -> {
                    if (state.getResolvedAt() == null) {
                        state.setResolvedAt(LocalDateTime.now());
                        alertStateRepository.save(state);
                    }
                });
    }

    private void notifyClinicOwner(Clinic clinic, NotificationType type, String message) {
        User owner = clinic.getOwner();
        if (owner == null) {
            return;
        }
        try {
            notificationLogRepository.save(NotificationLog.builder()
                    .user(owner)
                    .type(type)
                    .payload("{\"clinicUuid\":\"" + clinic.getUuid() + "\",\"message\":\""
                            + message.replace("\"", "'") + "\"}")
                    .sentAt(LocalDateTime.now())
                    .build());
            List<String> tokens = userFcmTokenRepository.findByUser(owner).stream()
                    .map(UserFcmToken::getToken)
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
            if (!tokens.isEmpty()) {
                fcmPushNotificationService.sendNotificationToUser(tokens, "Inventory alert", message);
            }
        } catch (Exception e) {
            log.warn("Inventory notify failed clinic={}: {}", clinic.getUuid(), e.getMessage());
        }
    }

    private static String strip(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        return v.stripTrailingZeros().toPlainString();
    }
}
