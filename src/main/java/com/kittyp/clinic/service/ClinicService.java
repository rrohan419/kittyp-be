package com.kittyp.clinic.service;

import java.util.List;

import com.kittyp.clinic.dto.ClinicDtos.AddVaccineDueRequest;
import com.kittyp.clinic.dto.ClinicDtos.ClinicalRecordRequest;
import com.kittyp.clinic.dto.ClinicDtos.MarkVaccineGivenRequest;
import com.kittyp.clinic.dto.ClinicDtos.VaccineCatalogModel;
import com.kittyp.clinic.dto.ClinicDtos.VaccineScheduleModel;
import com.kittyp.clinic.dto.ClinicDtos.AddOwnerPetRequest;
import com.kittyp.clinic.dto.ClinicDtos.AddPatientRequest;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicDoctorDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicOwnerModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicOwnerProfileModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicPetListModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicPetMedicalProfileModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicRequest;
import com.kittyp.clinic.dto.ClinicDtos.ClinicStatsModel;
import com.kittyp.clinic.dto.ClinicDtos.CreateOwnerRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteModel;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInvitePreview;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorLookupModel;
import com.kittyp.clinic.dto.ClinicDtos.DoctorModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventRequest;
import com.kittyp.clinic.dto.ClinicDtos.OwnerEmailLookupModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.PlatformUserSearchModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteCompleteRequest;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteModel;
import com.kittyp.clinic.dto.ClinicDtos.StaffInvitePreview;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteRequest;
import com.kittyp.clinic.dto.ClinicDtos.StaffMemberModel;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.common.model.PaginationModel;

public interface ClinicService {

    List<ClinicModel> mine(String email);

    /** Platform admin/moderator: every clinic, no membership filter. */
    List<ClinicModel> listAllClinics();

    ClinicModel create(ClinicRequest request, String email);

    ClinicModel get(String clinicUuid, String email);

    /** Platform admin/moderator: clinic by uuid without owner/staff/doctor membership. */
    ClinicModel getByUuidForAdmin(String clinicUuid);

    /** Platform admin/moderator: VERIFIED or REJECTED only. */
    ClinicModel updateStatusForAdmin(String clinicUuid, ClinicStatus status);

    ClinicModel update(String clinicUuid, ClinicRequest request, String email);

    /** Owner or staff/admin of this clinic (not affiliated-only doctors). */
    void requireClinicManager(String clinicUuid, String email);

    List<DoctorModel> doctors(String clinicUuid, String email);

    ClinicDoctorDetailModel doctorDetail(String clinicUuid, String doctorUuid, String email);

    DoctorInviteModel inviteDoctor(String clinicUuid, DoctorInviteRequest request, String email);

    DoctorLookupModel lookupDoctor(String doctorUuid, String email);

    List<DoctorInviteModel> listDoctorInvites(String clinicUuid, String email);

    /** Pending clinic invites addressed to the authenticated doctor's email. */
    List<DoctorInviteModel> listMyPendingInvites(String email);

    void revokeDoctorInvite(String clinicUuid, String inviteUuid, String email);

    DoctorInviteModel remindDoctorInvite(String clinicUuid, String inviteUuid, String email);

    DoctorInvitePreview previewInvite(String token);

    DoctorModel acceptInvite(String token, String email);

    void rejectInvite(String token, String email);

    StaffInviteModel inviteStaff(String clinicUuid, StaffInviteRequest request, String email);

    List<StaffInviteModel> listStaffInvites(String clinicUuid, String email);

    List<StaffMemberModel> listStaff(String clinicUuid, String email);

    void revokeStaffInvite(String clinicUuid, String inviteUuid, String email);

    void disableStaff(String clinicUuid, String userUuid, String email);

    StaffInvitePreview previewStaffInvite(String token);

    StaffMemberModel completeStaffInvite(String token, StaffInviteCompleteRequest request);

    List<PatientModel> patients(String clinicUuid, String email);

    PatientDetailModel patientDetail(String clinicUuid, String petUuid, String email);

    PatientDetailModel addPatient(String clinicUuid, AddPatientRequest request, String email);

    List<ClinicOwnerModel> listOwners(String clinicUuid, String q, String email, boolean emailOrIdOnly);

    PaginationModel<ClinicOwnerModel> pageOwners(String clinicUuid, String q, String email, boolean emailOrIdOnly,
            Integer pageNumber, Integer pageSize);

    /** Search all active KittyP users (live DB) for existing-customer pickers. */
    List<PlatformUserSearchModel> searchPlatformUsers(String clinicUuid, String q, String email,
            boolean emailOrIdOnly);

    /** Ensure a clinic client row exists for the given platform user and return it. */
    ClinicOwnerModel ensureOwnerFromUser(String clinicUuid, String userUuid, String email);

    /** Admit an existing KittyP (platform) pet into this clinic; idempotent. */
    ClinicPetListModel admitPlatformPet(String clinicUuid, String petUuid, String email);

    /** Lookup clinic owner and/or platform parent by email (New patient gate). */
    OwnerEmailLookupModel lookupOwnerByEmail(String clinicUuid, String ownerEmail, String email);

    void sendPetConsentOtp(String clinicUuid, String ownerUuid, String petName, String email);

    void verifyPetConsentOtp(String clinicUuid, String ownerUuid, String petName, String code, String email);

    /**
     * Consume one-pet email consent for scheduled booking / CRM add-pet.
     * Throws if OTP was not verified for this clinic + owner email + pet name.
     */
    void requirePetConsentForEmail(String clinicUuid, String ownerEmail, String petName);

    ClinicOwnerModel createOwner(String clinicUuid, CreateOwnerRequest request, String email);

    ClinicOwnerProfileModel ownerProfile(String clinicUuid, String ownerUuid, String email);

    ClinicPetListModel addPetToOwner(String clinicUuid, String ownerUuid, AddOwnerPetRequest request, String email);

    List<ClinicPetListModel> listPets(String clinicUuid, String q, String email, boolean emailOrIdOnly);

    PaginationModel<ClinicPetListModel> pagePets(String clinicUuid, String q, String email, boolean emailOrIdOnly,
            Integer pageNumber, Integer pageSize);

    ClinicPetMedicalProfileModel petMedicalProfile(String clinicUuid, String petUuid, String email);

    ClinicPetListModel updatePet(String clinicUuid, String petUuid, AddOwnerPetRequest request, String email);

    /** Soft-hide pet from clinic lists; row and visits remain. */
    void hidePet(String clinicUuid, String petUuid, String email);

    /** Soft-hide owner (+ their pets at this clinic) from lists; data retained. */
    void hideOwner(String clinicUuid, String ownerUuid, String email);

    PaginationModel<BookingModel> bookings(String clinicUuid, String status, int page, int size, String email);

    List<RetentionAlertModel> retentionAlerts(String clinicUuid, String email);

    void notifyAlert(String clinicUuid, String alertId, String email);

    List<HealthEventModel> healthEvents(String clinicUuid, String petUuid, String email);

    HealthEventModel createHealthEvent(String clinicUuid, String petUuid, HealthEventRequest request, String email);

    HealthEventModel createClinicalRecord(String clinicUuid, String petUuid, ClinicalRecordRequest request,
            String email);

    void assertClinicalUploadAllowed(String clinicUuid, String petUuid, String visitUuid, String email);

    List<VaccineCatalogModel> vaccineCatalog(String clinicUuid, String species, String email);

    VaccineScheduleModel addVaccineDue(String clinicUuid, String petUuid, AddVaccineDueRequest request, String email);

    VaccineScheduleModel markVaccineGiven(String clinicUuid, String petUuid, Long scheduleId,
            MarkVaccineGivenRequest request, String email);

    ClinicModel switchClinic(String clinicUuid, String email);

    ClinicModel shutdown(String clinicUuid, String email);

    ClinicModel reopen(String clinicUuid, String email);

    ClinicStatsModel stats(String clinicUuid, String email);
}
