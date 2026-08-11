package com.kittyp.visit.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
            String gender,
            String photoUrl) {
    }

    public record WalkInCreateRequest(
            String petUuid,
            @Valid WalkInOwnerRequest owner,
            @Valid WalkInPetRequest newPet,
            String reasonForVisit,
            VisitUrgency urgency,
            String doctorUuid) {
    }

    /** Clinic front-desk schedule: creates a Booking with a future slot. */
    public record ScheduleBookingCreateRequest(
            String petUuid,
            @Valid WalkInOwnerRequest owner,
            @Valid WalkInPetRequest newPet,
            @NotBlank String doctorUuid,
            @NotNull LocalDateTime slotStart,
            LocalDateTime slotEnd,
            Integer durationMinutes,
            String notes,
            BookingMode mode) {
    }

    /** Parent self-book: owned pet + clinic doctor + slot. */
    public record ParentBookingCreateRequest(
            @NotBlank String clinicUuid,
            @NotBlank String doctorUuid,
            @NotBlank String petUuid,
            @NotNull LocalDateTime slotStart,
            String notes,
            BookingMode mode) {
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
            LocalDateTime checkingOutAt,
            LocalDateTime createdAt,
            VisitChartModel chart,
            String invoiceUuid,
            String healthEventUuid,
            Integer parentRating) {
        public VisitModel(
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
            this(uuid, clinicUuid, clinicName, petUuid, petName, ownerName, ownerEmail, ownerPhone, doctorUuid,
                    doctorName, doctorSpecialization, doctorExperienceYears, source, channel, status, urgency,
                    reasonForVisit, checkedInAt, startedAt, completedAt, null, createdAt, chart, invoiceUuid,
                    healthEventUuid, null);
        }
    }

    public record VisitRatingRequest(
            @NotNull @Min(1) @Max(5) Integer stars,
            @Size(max = 1000) String comment) {
    }

    public record VisitRatingModel(
            String visitUuid,
            String doctorUuid,
            Integer stars,
            String ratingLabel,
            Double doctorRating,
            Integer doctorReviewsCount) {
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
