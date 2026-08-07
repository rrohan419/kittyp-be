package com.kittyp.visit.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class VisitDtos {

    private VisitDtos() {
    }

    public record WalkInOwnerRequest(
            @NotBlank String firstName,
            String lastName,
            @NotBlank @Email String email,
            @NotBlank String phone,
            String address) {
    }

    public record WalkInPetRequest(
            @NotBlank String name,
            String species,
            String breed,
            String gender) {
    }

    public record WalkInCreateRequest(
            String petUuid,
            @Valid WalkInOwnerRequest owner,
            @Valid WalkInPetRequest newPet,
            String reasonForVisit,
            VisitUrgency urgency,
            String doctorUuid) {
    }

    public record VisitPatchRequest(
            VisitStatus status,
            String doctorUuid,
            VisitUrgency urgency,
            String reasonForVisit) {
    }

    public record VisitChartRequest(
            String examinationNotes,
            String assessment,
            String plan,
            String nextVisitNotes,
            Map<String, Object> vitals,
            String internalNotes) {
    }

    public record VisitChartModel(
            String examinationNotes,
            String assessment,
            String plan,
            String nextVisitNotes,
            Map<String, Object> vitals,
            String internalNotes) {
    }

    public record VisitModel(
            String uuid,
            String clinicUuid,
            String clinicName,
            String petUuid,
            String petName,
            String ownerName,
            String ownerEmail,
            String ownerPhone,
            String doctorUuid,
            String doctorName,
            String doctorSpecialization,
            Double doctorExperienceYears,
            VisitSource source,
            BookingMode channel,
            VisitStatus status,
            VisitUrgency urgency,
            String reasonForVisit,
            LocalDateTime checkedInAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            VisitChartModel chart,
            String invoiceUuid,
            String healthEventUuid) {
    }

    /** Aggregated pet + owner rows for doctor/clinic "attended patients" lists. */
    public record AttendedPatientModel(
            String petUuid,
            String petName,
            String species,
            String breed,
            String ownerUuid,
            String ownerName,
            String ownerEmail,
            String ownerPhone,
            String clinicUuid,
            String clinicName,
            int visitCount,
            LocalDateTime lastVisitAt,
            String lastAssessment) {
    }
}
