package com.kittyp.email.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.entity.ClinicEmailActivity;
import com.kittyp.email.entity.ClinicEmailActivity.Status;
import com.kittyp.email.model.ClinicEmailUsageModel;
import com.kittyp.email.model.ClinicEmailUsageModel.EmailActivityModel;
import com.kittyp.email.repository.ClinicEmailActivityRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicEmailUsageService {
    public static final int MONTHLY_ALLOWANCE = 2_000;

    private static final List<Status> RESERVED_OR_ACCEPTED = List.of(Status.RESERVED, Status.ACCEPTED);

    private final ClinicRepository clinics;
    private final ClinicEmailActivityRepository activities;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(Long clinicId, String recipientEmail, String emailType) {
        Clinic clinic = clinics.lockById(clinicId)
                .orElseThrow(() -> new CustomException("Clinic not found", HttpStatus.NOT_FOUND));
        LocalDate periodStart = currentPeriodStart();
        long reservedOrSent = activities.countByClinic_IdAndPeriodStartAndStatusIn(
                clinicId, periodStart, RESERVED_OR_ACCEPTED);

        ClinicEmailActivity activity = new ClinicEmailActivity();
        activity.setClinic(clinic);
        activity.setPeriodStart(periodStart);
        activity.setRecipientEmail(limit(recipientEmail.trim(), 320));
        activity.setEmailType(limit(emailType, 160));
        if (reservedOrSent >= MONTHLY_ALLOWANCE) {
            activity.setStatus(Status.BLOCKED);
            activities.save(activity);
            return new Reservation(false, null);
        }

        activity.setStatus(Status.RESERVED);
        activity = activities.save(activity);
        return new Reservation(true, activity.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAccepted(Long activityId, String providerRequestId) {
        ClinicEmailActivity activity = requireReserved(activityId);
        activity.setStatus(Status.ACCEPTED);
        activity.setProviderRequestId(limitToNull(providerRequestId, 160));
        activities.save(activity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long activityId, String failureCode) {
        ClinicEmailActivity activity = requireReserved(activityId);
        activity.setStatus(Status.FAILED);
        activity.setFailureCode(limitToNull(failureCode, 80));
        activities.save(activity);
    }

    @Transactional(readOnly = true)
    public ClinicEmailUsageModel getUsage(Long clinicId, int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new CustomException("Invalid email activity page", HttpStatus.BAD_REQUEST);
        }
        LocalDate periodStart = currentPeriodStart();
        long sent = activities.countByClinic_IdAndPeriodStartAndStatus(clinicId, periodStart, Status.ACCEPTED);
        long reserved = activities.countByClinic_IdAndPeriodStartAndStatus(clinicId, periodStart, Status.RESERVED);
        Page<ClinicEmailActivity> page = activities.findAllByClinic_IdOrderByCreatedAtDesc(
                clinicId, PageRequest.of(pageNumber, pageSize, Sort.unsorted()));
        List<EmailActivityModel> items = page.getContent().stream()
                .map(item -> new EmailActivityModel(item.getId(), item.getRecipientEmail(), item.getEmailType(),
                        item.getStatus(), item.getFailureCode(), item.getCreatedAt()))
                .toList();
        return new ClinicEmailUsageModel(MONTHLY_ALLOWANCE, sent, reserved,
                Math.max(0, MONTHLY_ALLOWANCE - sent - reserved), periodStart,
                periodStart.plusMonths(1), items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private ClinicEmailActivity requireReserved(Long activityId) {
        ClinicEmailActivity activity = activities.findById(activityId)
                .orElseThrow(() -> new IllegalStateException("Email activity reservation not found"));
        if (activity.getStatus() != Status.RESERVED) {
            throw new IllegalStateException("Email activity is no longer reserved");
        }
        return activity;
    }

    private static LocalDate currentPeriodStart() {
        return YearMonth.now(ZoneOffset.UTC).atDay(1);
    }

    private static String limit(String value, int length) {
        String normalized = value == null || value.isBlank() ? "EMAIL" : value.trim();
        return normalized.length() <= length ? normalized : normalized.substring(0, length);
    }

    private static String limitToNull(String value, int length) {
        return value == null || value.isBlank() ? null : limit(value, length);
    }

    public record Reservation(boolean allowed, Long activityId) {
    }
}
