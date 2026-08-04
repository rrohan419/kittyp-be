package com.kittyp.clinic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ClinicDtos {

    private ClinicDtos() {
    }

    public record ClinicRequest(@NotBlank String name, String licenseNumber, String address, String phone, String email,
            String timezone, String operatingHours) {
    }

    public record ClinicModel(String uuid, String name, String licenseNumber, String address, String phone, String email,
            String timezone, String operatingHours, String status) {
    }

    public record DoctorModel(String doctorUuid, String userUuid, String name, String email, String specialization,
            String role, Boolean isActive) {
    }

    public record PatientModel(String petUuid, String petName, String ownerName, String ownerEmail,
            LocalDateTime lastVisit) {
    }

    public record VaccineScheduleModel(Long id, String vaccineName, LocalDate dueDate, Boolean completed,
            LocalDate completedDate) {
    }

    public record HealthEventModel(String uuid, String type, String title, String description, LocalDate date,
            Boolean isPast, String status, List<String> attachments) {
    }

    public record PatientDetailModel(PatientModel patient, List<HealthEventModel> healthEvents,
            List<VaccineScheduleModel> vaccineSchedule) {
    }

    public record BookingModel(String uuid, String petUuid, String petName, String ownerName, String doctorUuid,
            LocalDateTime slotStart, LocalDateTime slotEnd, String timezone, BookingStatus status, String mode,
            String notes) {
    }

    public record RetentionAlertModel(String id, String petUuid, String petName, String ownerName, String type,
            String message, long dueInDays, String status) {
    }

    public record HealthEventRequest(@NotNull HealthEventType type, String title, String description,
            @NotNull LocalDate date, Boolean isPast, HealthEventStatus status, List<String> attachments) {
    }
}
