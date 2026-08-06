package com.kittyp.clinic.service;

import java.util.List;

import com.kittyp.clinic.dto.ClinicDtos.AddOwnerPetRequest;
import com.kittyp.clinic.dto.ClinicDtos.AddPatientRequest;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
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
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.common.model.PaginationModel;

public interface ClinicService {

    List<ClinicModel> mine(String email);

    ClinicModel create(ClinicRequest request, String email);

    ClinicModel get(String clinicUuid, String email);

    ClinicModel update(String clinicUuid, ClinicRequest request, String email);

    List<DoctorModel> doctors(String clinicUuid, String email);

    DoctorInviteModel inviteDoctor(String clinicUuid, DoctorInviteRequest request, String email);

    DoctorLookupModel lookupDoctor(String doctorUuid, String email);

    List<DoctorInviteModel> listDoctorInvites(String clinicUuid, String email);

    void revokeDoctorInvite(String clinicUuid, String inviteUuid, String email);

    DoctorInvitePreview previewInvite(String token);

    DoctorModel acceptInvite(String token, String email);

    List<PatientModel> patients(String clinicUuid, String email);

    PatientDetailModel patientDetail(String clinicUuid, String petUuid, String email);

    PatientDetailModel addPatient(String clinicUuid, AddPatientRequest request, String email);

    List<ClinicOwnerModel> listOwners(String clinicUuid, String q, String email);

    ClinicOwnerModel createOwner(String clinicUuid, CreateOwnerRequest request, String email);

    ClinicOwnerProfileModel ownerProfile(String clinicUuid, String ownerUuid, String email);

    ClinicPetListModel addPetToOwner(String clinicUuid, String ownerUuid, AddOwnerPetRequest request, String email);

    List<ClinicPetListModel> listPets(String clinicUuid, String q, String email);

    ClinicPetMedicalProfileModel petMedicalProfile(String clinicUuid, String petUuid, String email);

    PaginationModel<BookingModel> bookings(String clinicUuid, String status, int page, int size, String email);

    List<RetentionAlertModel> retentionAlerts(String clinicUuid, String email);

    void notifyAlert(String clinicUuid, String alertId, String email);

    List<HealthEventModel> healthEvents(String clinicUuid, String petUuid, String email);

    HealthEventModel createHealthEvent(String clinicUuid, String petUuid, HealthEventRequest request, String email);

    ClinicModel switchClinic(String clinicUuid, String email);

    ClinicModel shutdown(String clinicUuid, String email);

    ClinicModel reopen(String clinicUuid, String email);

    ClinicStatsModel stats(String clinicUuid, String email);
}
