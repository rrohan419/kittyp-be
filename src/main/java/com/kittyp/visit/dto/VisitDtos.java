package com.kittyp.visit.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
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

    /** Clinic front-desk schedule: creates a Booking with a future slot. Doctor optional (unassigned). */
    public record ScheduleBookingCreateRequest(
            String petUuid,
            @Valid WalkInOwnerRequest owner,
            @Valid WalkInPetRequest newPet,
            String doctorUuid,
            @NotNull LocalDateTime slotStart,
            LocalDateTime slotEnd,
            Integer durationMinutes,
            String notes,
            BookingMode mode) {
    }

    /** Clinic front-desk reschedule / cancel. Null fields stay unchanged. */
    public record ScheduleBookingPatchRequest(
            String doctorUuid,
            LocalDateTime slotStart,
            String notes,
            BookingStatus status,
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
            String species,
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
            this(uuid, clinicUuid, clinicName, petUuid, petName, null, ownerName, ownerEmail, ownerPhone, doctorUuid,
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
    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
    public static final class AttendedPatientModel {
        private final String petUuid;
        private final String petName;
        private final String species;
        private final String breed;
        private final LocalDate dateOfBirth;
        private final String weight;
        private final String profilePicture;
        private final String activityLevel;
        private final String gender;
        private final String currentFoodBrand;
        private final String healthConditions;
        private final String allergies;
        private final boolean isNeutered;
        private final String ownerUuid;
        private final String ownerName;
        private final String ownerEmail;
        private final String ownerPhone;
        private final String clinicUuid;
        private final String clinicName;
        private final int visitCount;
        private final LocalDateTime lastVisitAt;
        private final String lastAssessment;

        public AttendedPatientModel(
                String petUuid,
                String petName,
                String species,
                String breed,
                LocalDate dateOfBirth,
                String weight,
                String profilePicture,
                String activityLevel,
                String gender,
                String currentFoodBrand,
                String healthConditions,
                String allergies,
                boolean isNeutered,
                String ownerUuid,
                String ownerName,
                String ownerEmail,
                String ownerPhone,
                String clinicUuid,
                String clinicName,
                int visitCount,
                LocalDateTime lastVisitAt,
                String lastAssessment) {
            this.petUuid = petUuid;
            this.petName = petName;
            this.species = species;
            this.breed = breed;
            this.dateOfBirth = dateOfBirth;
            this.weight = weight;
            this.profilePicture = profilePicture;
            this.activityLevel = activityLevel;
            this.gender = gender;
            this.currentFoodBrand = currentFoodBrand;
            this.healthConditions = healthConditions;
            this.allergies = allergies;
            this.isNeutered = isNeutered;
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.ownerEmail = ownerEmail;
            this.ownerPhone = ownerPhone;
            this.clinicUuid = clinicUuid;
            this.clinicName = clinicName;
            this.visitCount = visitCount;
            this.lastVisitAt = lastVisitAt;
            this.lastAssessment = lastAssessment;
        }

        public String petUuid() { return petUuid; }
        public String petName() { return petName; }
        public String species() { return species; }
        public String breed() { return breed; }
        public LocalDate dateOfBirth() { return dateOfBirth; }
        public String weight() { return weight; }
        public String profilePicture() { return profilePicture; }
        public String activityLevel() { return activityLevel; }
        public String gender() { return gender; }
        public String currentFoodBrand() { return currentFoodBrand; }
        public String healthConditions() { return healthConditions; }
        public String allergies() { return allergies; }
        public boolean isNeutered() { return isNeutered; }
        public String ownerUuid() { return ownerUuid; }
        public String ownerName() { return ownerName; }
        public String ownerEmail() { return ownerEmail; }
        public String ownerPhone() { return ownerPhone; }
        public String clinicUuid() { return clinicUuid; }
        public String clinicName() { return clinicName; }
        public int visitCount() { return visitCount; }
        public LocalDateTime lastVisitAt() { return lastVisitAt; }
        public String lastAssessment() { return lastAssessment; }
    }
}
