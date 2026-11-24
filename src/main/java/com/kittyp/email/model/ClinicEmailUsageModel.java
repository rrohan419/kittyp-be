package com.kittyp.email.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.kittyp.email.entity.ClinicEmailActivity.Status;

public record ClinicEmailUsageModel(
        int monthlyAllowance,
        long sentThisMonth,
        long reservedThisMonth,
        long availableCredits,
        LocalDate periodStart,
        LocalDate nextResetDate,
        List<EmailActivityModel> activities,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages) {

    public record EmailActivityModel(
            Long id,
            String recipientEmail,
            String emailType,
            Status status,
            String failureCode,
            LocalDateTime createdAt) {
    }
}
