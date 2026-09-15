package com.kittyp.clinic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ClinicDtos {

    private ClinicDtos() {
    }

    public record ClinicRequest(@NotBlank String name, String licenseNumber, String address, String phone, String email,
            String timezone, String operatingHours, String city, Double latitude, Double longitude,
            String profileImageUrl) {
        public ClinicRequest(@NotBlank String name, String licenseNumber, String address, String phone, String email,
                String timezone, String operatingHours) {
            this(name, licenseNumber, address, phone, email, timezone, operatingHours, null, null, null, null);
        }
    }

    public record ClinicModel(String uuid, String name, String licenseNumber, String address, String phone, String email,
            String timezone, String operatingHours, String status, Boolean personal, Boolean whatsappConfigured,
            String city, Double latitude, Double longitude, String profileImageUrl) {
        /** Backward-compatible ctor without personal / whatsapp / geo flags. */
        public ClinicModel(String uuid, String name, String licenseNumber, String address, String phone, String email,
                String timezone, String operatingHours, String status) {
            this(uuid, name, licenseNumber, address, phone, email, timezone, operatingHours, status, false, false,
                    null, null, null, null);
        }

        public ClinicModel(String uuid, String name, String licenseNumber, String address, String phone, String email,
                String timezone, String operatingHours, String status, Boolean personal) {
            this(uuid, name, licenseNumber, address, phone, email, timezone, operatingHours, status, personal, false,
                    null, null, null, null);
        }

        public ClinicModel(String uuid, String name, String licenseNumber, String address, String phone, String email,
                String timezone, String operatingHours, String status, Boolean personal, Boolean whatsappConfigured) {
            this(uuid, name, licenseNumber, address, phone, email, timezone, operatingHours, status, personal,
                    whatsappConfigured, null, null, null, null);
        }
    }

    public record ClinicStatusUpdateRequest(@NotNull ClinicStatus status) {
    }

    public record DoctorModel(String doctorUuid, String userUuid, String name, String email, String specialization,
            String role, Boolean isActive, String status, String photoUrl, Double rating, Integer reviewsCount,
            String ratingLabel, String joinedAt, Double experienceYears, String registrationNumber, String clinicUuid) {
        public DoctorModel(String doctorUuid, String userUuid, String name, String email, String specialization,
                String role, Boolean isActive) {
            this(doctorUuid, userUuid, name, email, specialization, role, isActive, null, null, null, null, null,
                    null, null, null, null);
        }

        public DoctorModel(String doctorUuid, String userUuid, String name, String email, String specialization,
                String role, Boolean isActive, String status, String photoUrl) {
            this(doctorUuid, userUuid, name, email, specialization, role, isActive, status, photoUrl, null, null,
                    null, null, null, null, null);
        }
    }

    /** Full doctor profile for clinic staff (read-only verification + related patients). */
    public record ClinicDoctorDetailModel(
            String doctorUuid,
            String userUuid,
            String name,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String specialization,
            String registrationNumber,
            String licenseNumber,
            String bio,
            String photoUrl,
            Double experienceYears,
            String role,
            Boolean isActive,
            String joinedAt,
            String status,
            String degreeCertificateUrl,
            String registrationCertificateUrl,
            String governmentIdUrl,
            String licenseDocumentUrl,
            String clinicPhotosUrls,
            boolean emailOtpVerified,
            boolean phoneOtpVerified,
            boolean checkMobileOtp,
            boolean checkEmailOtp,
            boolean checkGovernmentId,
            boolean checkDegree,
            boolean checkRegistrationCertificate,
            boolean checkClinicAddress,
            boolean checkRegistrationNumber,
            boolean checkGoogleMapsMatch,
            boolean checkClinicPhotos,
            String submittedAt,
            String reviewedAt,
            String reviewNotes,
            Double rating,
            Integer reviewsCount,
            String ratingLabel,
            List<ClinicDoctorPatientModel> patients) {
    }

    public record ClinicDoctorPatientModel(ClinicPetListModel pet, OwnerSummaryModel owner, int appointmentCount,
            LocalDateTime lastAppointment) {
    }

    public record PatientModel(String petUuid, String petName, String ownerName, String ownerEmail,
            LocalDateTime lastVisit, String ownerUuid, String ownerPhone, String ownerAddress, String species,
            String breed) {
    }

    public record OwnerSummaryModel(String ownerUuid, String name, String email, String phone, String address,
            Boolean linked, String linkedUserUuid) {
        public OwnerSummaryModel(String ownerUuid, String name, String email, String phone, String address) {
            this(ownerUuid, name, email, phone, address, null, null);
        }
    }

    public record PatientPetModel(String petUuid, String petName, String species, String breed,
            LocalDateTime lastVisit, boolean atThisClinic, String globalPetId, String microchipNumber) {
        public PatientPetModel(String petUuid, String petName, String species, String breed,
                LocalDateTime lastVisit, boolean atThisClinic) {
            this(petUuid, petName, species, breed, lastVisit, atThisClinic, null, null);
        }
    }

    public record VaccineScheduleModel(Long id, String vaccineName, LocalDate dueDate, Boolean completed,
            LocalDate completedDate, String certificateUrl) {
        public VaccineScheduleModel(Long id, String vaccineName, LocalDate dueDate, Boolean completed,
                LocalDate completedDate) {
            this(id, vaccineName, dueDate, completed, completedDate, null);
        }
    }

    public record VaccineCatalogModel(Long id, String name, String species, String description) {
    }

    public record AddVaccineDueRequest(@NotNull Long vaccineMasterId, @NotNull LocalDate dueDate) {
    }

    public record MarkVaccineGivenRequest(LocalDate completedDate, String certificateUrl) {
    }

    public record HealthEventModel(String uuid, String type, String title, String description, LocalDate date,
            Boolean isPast, String status, List<String> attachments, String visitUuid) {
        public HealthEventModel(String uuid, String type, String title, String description, LocalDate date,
                Boolean isPast, String status, List<String> attachments) {
            this(uuid, type, title, description, date, isPast, status, attachments, null);
        }
    }

    public record ClinicalRecordModel(String uuid, String title, String description, LocalDate date, String visitUuid,
            List<String> attachments) {
    }

    public record PatientDetailModel(PatientModel patient, OwnerSummaryModel owner, List<PatientPetModel> pets,
            List<HealthEventModel> healthEvents, List<VaccineScheduleModel> vaccineSchedule) {
    }

    public record ClinicOwnerPetModel(String petUuid, String globalPetId, String name, String species, String breed,
            String gender, LocalDate dateOfBirth, String weight, String microchipNumber, String photoUrl,
            String patientNumber, LocalDateTime lastVisit) {
    }

    public record ClinicOwnerModel(String ownerUuid, String name, String firstName, String lastName, String email,
            String phone, String alternatePhone, String address, String notes, boolean linked, String linkedUserUuid,
            int petCount, LocalDateTime lastVisit, List<ClinicOwnerPetModel> pets) {
    }

    /** KittyP platform user hit for clinic "existing customer" search. */
    public record PlatformUserSearchModel(
            String userUuid,
            String name,
            String email,
            String phone,
            String clinicOwnerUuid,
            boolean alreadyClient) {
    }

    public record EnsureOwnerFromUserRequest(@NotBlank String userUuid) {
    }

    public record AdmitPlatformPetRequest(@NotBlank String petUuid) {
    }

    /** Email gate for New patient: existing clinic owner and/or KittyP parent. */
    public record OwnerEmailLookupModel(
            boolean found,
            String source,
            ClinicOwnerModel owner,
            PlatformUserSearchModel platformUser) {
    }

    public record PetConsentSendRequest(@NotBlank String petName) {
    }

    public record PetConsentVerifyRequest(@NotBlank String petName, @NotBlank String code) {
    }

    public record ClinicOwnerProfileModel(ClinicOwnerModel owner, String billingStatus, long invoiceCount) {
    }

    public record ClinicPetListModel(String petUuid, String globalPetId, String name, String species, String breed,
            String gender, LocalDate dateOfBirth, String weight, String microchipNumber, String photoUrl,
            String patientNumber, String ownerUuid, String ownerName, String ownerPhone, String ownerEmail,
            boolean linked, LocalDateTime lastVisit) {
    }

    public record ClinicPetMedicalProfileModel(ClinicPetListModel pet, OwnerSummaryModel owner,
            List<HealthEventModel> timeline, List<BookingModel> appointments, List<VaccineScheduleModel> vaccinations,
            List<String> prescriptions, List<ClinicalRecordModel> labReports, List<ClinicalRecordModel> surgeries,
            List<InvoiceSummaryModel> invoices) {
    }

    public record InvoiceSummaryModel(String uuid, String status, String amount, String currency, String petUuid,
            LocalDateTime createdAt) {
    }

    public record CreateOwnerRequest(
            @NotBlank String firstName,
            String lastName,
            @NotBlank @Email String email,
            @NotBlank String phone,
            String alternatePhone,
            String address,
            String notes) {
    }

    public record AddOwnerPetRequest(
            @NotBlank String name,
            String species,
            String breed,
            String gender,
            LocalDate dateOfBirth,
            String weight,
            String microchipNumber,
            String photoUrl,
            String patientNumber) {
    }

    public record BookingModel(String uuid, String petUuid, String petName, String ownerName, String doctorUuid,
            LocalDateTime slotStart, LocalDateTime slotEnd, String timezone, BookingStatus status, String mode,
            String notes, String clinicUuid, String clinicName, String doctorName, String doctorSpecialization,
            String doctorPhotoUrl, String species, String videoJoinUrl) {
        public BookingModel(String uuid, String petUuid, String petName, String ownerName, String doctorUuid,
                LocalDateTime slotStart, LocalDateTime slotEnd, String timezone, BookingStatus status, String mode,
                String notes) {
            this(uuid, petUuid, petName, ownerName, doctorUuid, slotStart, slotEnd, timezone, status, mode, notes,
                    null, null, null, null, null, null, null);
        }

        public BookingModel(String uuid, String petUuid, String petName, String ownerName, String doctorUuid,
                LocalDateTime slotStart, LocalDateTime slotEnd, String timezone, BookingStatus status, String mode,
                String notes, String clinicUuid) {
            this(uuid, petUuid, petName, ownerName, doctorUuid, slotStart, slotEnd, timezone, status, mode, notes,
                    clinicUuid, null, null, null, null, null, null);
        }

        public BookingModel(String uuid, String petUuid, String petName, String ownerName, String doctorUuid,
                LocalDateTime slotStart, LocalDateTime slotEnd, String timezone, BookingStatus status, String mode,
                String notes, String clinicUuid, String clinicName) {
            this(uuid, petUuid, petName, ownerName, doctorUuid, slotStart, slotEnd, timezone, status, mode, notes,
                    clinicUuid, clinicName, null, null, null, null, null);
        }
    }

    public record RetentionAlertModel(String id, String petUuid, String petName, String ownerName, String type,
            String message, long dueInDays, String status) {
    }

    public record HealthEventRequest(@NotNull HealthEventType type, String title, String description,
            @NotNull LocalDate date, Boolean isPast, HealthEventStatus status, List<String> attachments,
            String visitUuid) {
        public HealthEventRequest(@NotNull HealthEventType type, String title, String description,
                @NotNull LocalDate date, Boolean isPast, HealthEventStatus status, List<String> attachments) {
            this(type, title, description, date, isPast, status, attachments, null);
        }
    }

    public record ClinicalRecordRequest(@NotNull HealthEventType type, String title, String description,
            @NotNull LocalDate date, String visitUuid, List<String> attachments) {
    }

    public record SwitchClinicRequest(@NotBlank String clinicUuid) {
    }

    public record ClinicStatsModel(long diagnosedPetCount, long patientCount, Double clinicRating,
            Long clinicReviewsCount, String clinicRatingLabel) {
        public ClinicStatsModel(long diagnosedPetCount, long patientCount) {
            this(diagnosedPetCount, patientCount, null, 0L, "Not rated yet");
        }
    }

    public record DoctorInviteRequest(String name, String email, String doctorUuid) {
    }

    public record DoctorLookupModel(String doctorUuid, String name, String email) {
    }

    public record DoctorInviteModel(String uuid, String email, String doctorName, String status, String expiresAt,
            String clinicUuid, String clinicName, String token, String createdAt, String lastRemindedAt,
            Boolean canRemind) {
    }

    public record DoctorInvitePreview(String clinicName, String doctorName, String email, boolean expired,
            boolean accepted, String status, String clinicUuid) {
    }

    public record StaffInviteRequest(@NotBlank String name, @NotBlank @Email String email) {
    }

    public record StaffInviteCompleteRequest(
            @NotBlank @Size(min = 2, max = 50) String firstName,
            @Size(max = 50) String lastName,
            @NotBlank @Size(min = 8, max = 72) @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
                    message = "Password must be 8-72 chars with upper, lower, digit, and special character") String password) {
    }

    public record StaffInviteModel(String uuid, String email, String staffName, String status, String expiresAt,
            String clinicUuid, String clinicName, String token, String createdAt) {
    }

    public record StaffInvitePreview(String clinicName, String staffName, String email, boolean expired,
            boolean accepted, String status) {
    }

    public record StaffMemberModel(String userUuid, String email, String name, String role, String joinedAt,
            boolean enabled) {
    }

    public record AddPatientRequest(
            @NotBlank String ownerFirstName,
            String ownerLastName,
            @NotBlank @Email String ownerEmail,
            @NotBlank String ownerPhone,
            String ownerAddress,
            String ownerAlternatePhone,
            String ownerNotes,
            @NotBlank String petName,
            String petType,
            String petBreed,
            String petGender,
            LocalDate petDateOfBirth,
            String petWeight,
            String petMicrochipNumber,
            String petPhotoUrl) {
    }
}
