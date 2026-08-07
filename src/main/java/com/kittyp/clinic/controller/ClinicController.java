package com.kittyp.clinic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import com.kittyp.clinic.dto.ClinicDtos.CreateOwnerRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteModel;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInvitePreview;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorLookupModel;
import com.kittyp.clinic.dto.ClinicDtos.DoctorModel;
import com.kittyp.clinic.dto.ClinicDtos.EnsureOwnerFromUserRequest;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventRequest;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.PlatformUserSearchModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicStatsModel;
import com.kittyp.clinic.dto.ClinicDtos.SwitchClinicRequest;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.common.model.PaginationModel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicController {

    private static final String CLINIC_ACCESS = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF + " or " + KeyConstant.IS_ROLE_DOCTOR;

    private final ClinicService clinicService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.CLINIC_MINE)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<ClinicModel>>> mine() {
        return success(clinicService.mine(email()));
    }

    @PostMapping(ApiUrl.CLINIC_BASE_URL)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> create(@RequestBody @Valid ClinicRequest request) {
        return success(clinicService.create(request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_BY_UUID)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> detail(@PathVariable String uuid) {
        return success(clinicService.get(uuid, email()));
    }

    @PatchMapping(ApiUrl.CLINIC_BY_UUID)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> update(@PathVariable String uuid,
            @RequestBody @Valid ClinicRequest request) {
        return success(clinicService.update(uuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_DOCTORS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<DoctorModel>>> doctors(@PathVariable String uuid) {
        return success(clinicService.doctors(uuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_DOCTOR_BY_UUID)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicDoctorDetailModel>> doctorDetail(
            @PathVariable String uuid, @PathVariable String doctorUuid) {
        return success(clinicService.doctorDetail(uuid, doctorUuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_DOCTOR_INVITE)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<DoctorInviteModel>> inviteDoctor(@PathVariable String uuid,
            @RequestBody @Valid DoctorInviteRequest request) {
        return success(clinicService.inviteDoctor(uuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_DOCTOR_LOOKUP)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<DoctorLookupModel>> lookupDoctor(@RequestParam String uuid) {
        return success(clinicService.lookupDoctor(uuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_DOCTOR_INVITES)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<DoctorInviteModel>>> doctorInvites(@PathVariable String uuid) {
        return success(clinicService.listDoctorInvites(uuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_DOCTOR_INVITE_REVOKE)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<Void>> revokeDoctorInvite(@PathVariable String uuid,
            @PathVariable String inviteUuid) {
        clinicService.revokeDoctorInvite(uuid, inviteUuid, email());
        return success(null);
    }

    @PostMapping(ApiUrl.CLINIC_DOCTOR_INVITE_REMIND)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<DoctorInviteModel>> remindDoctorInvite(@PathVariable String uuid,
            @PathVariable String inviteUuid) {
        return success(clinicService.remindDoctorInvite(uuid, inviteUuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_MY_INVITES)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<DoctorInviteModel>>> myPendingInvites() {
        return success(clinicService.listMyPendingInvites(email()));
    }

    @GetMapping(ApiUrl.CLINIC_INVITE_BY_TOKEN)
    public ResponseEntity<SuccessResponse<DoctorInvitePreview>> previewInvite(@PathVariable String token) {
        return success(clinicService.previewInvite(token));
    }

    @PostMapping(ApiUrl.CLINIC_INVITE_ACCEPT)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<DoctorModel>> acceptInvite(@PathVariable String token) {
        return success(clinicService.acceptInvite(token, email()));
    }

    @PostMapping(ApiUrl.CLINIC_INVITE_REJECT)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<Void>> rejectInvite(@PathVariable String token) {
        clinicService.rejectInvite(token, email());
        return success(null);
    }

    @GetMapping(ApiUrl.CLINIC_PATIENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<PatientModel>>> patients(@PathVariable String uuid) {
        return success(clinicService.patients(uuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_PATIENTS)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN + " or " + KeyConstant.IS_ROLE_CLINIC_STAFF + " or "
            + KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<PatientDetailModel>> addPatient(@PathVariable String uuid,
            @RequestBody @Valid AddPatientRequest request) {
        return success(clinicService.addPatient(uuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_PATIENT_DETAIL)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<PatientDetailModel>> patient(@PathVariable String uuid,
            @PathVariable String petUuid) {
        return success(clinicService.patientDetail(uuid, petUuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_OWNERS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<ClinicOwnerModel>>> owners(@PathVariable String uuid,
            @RequestParam(required = false) String q) {
        return success(clinicService.listOwners(uuid, q, email()));
    }

    @GetMapping(ApiUrl.CLINIC_USERS_SEARCH)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<PlatformUserSearchModel>>> searchUsers(@PathVariable String uuid,
            @RequestParam(required = false) String q) {
        return success(clinicService.searchPlatformUsers(uuid, q, email()));
    }

    @PostMapping(ApiUrl.CLINIC_OWNER_FROM_USER)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN + " or " + KeyConstant.IS_ROLE_CLINIC_STAFF + " or "
            + KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ClinicOwnerModel>> ownerFromUser(@PathVariable String uuid,
            @RequestBody @Valid EnsureOwnerFromUserRequest request) {
        return success(clinicService.ensureOwnerFromUser(uuid, request.userUuid(), email()));
    }

    @PostMapping(ApiUrl.CLINIC_OWNERS)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN + " or " + KeyConstant.IS_ROLE_CLINIC_STAFF + " or "
            + KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ClinicOwnerModel>> createOwner(@PathVariable String uuid,
            @RequestBody @Valid CreateOwnerRequest request) {
        return success(clinicService.createOwner(uuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_OWNER_DETAIL)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicOwnerProfileModel>> ownerProfile(@PathVariable String uuid,
            @PathVariable String ownerUuid) {
        return success(clinicService.ownerProfile(uuid, ownerUuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_OWNER_PETS)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN + " or " + KeyConstant.IS_ROLE_CLINIC_STAFF + " or "
            + KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<ClinicPetListModel>> addPetToOwner(@PathVariable String uuid,
            @PathVariable String ownerUuid, @RequestBody @Valid AddOwnerPetRequest request) {
        return success(clinicService.addPetToOwner(uuid, ownerUuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_PETS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<ClinicPetListModel>>> pets(@PathVariable String uuid,
            @RequestParam(required = false) String q) {
        return success(clinicService.listPets(uuid, q, email()));
    }

    @GetMapping(ApiUrl.CLINIC_PET_DETAIL)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicPetMedicalProfileModel>> petProfile(@PathVariable String uuid,
            @PathVariable String petUuid) {
        return success(clinicService.petMedicalProfile(uuid, petUuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_PET_HIDE)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN + " or " + KeyConstant.IS_ROLE_CLINIC_STAFF + " or "
            + KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<MessageResponse>> hidePet(@PathVariable String uuid,
            @PathVariable String petUuid) {
        clinicService.hidePet(uuid, petUuid, email());
        return success(new MessageResponse("Pet hidden from clinic lists (records kept)"));
    }

    @PostMapping(ApiUrl.CLINIC_OWNER_HIDE)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN + " or " + KeyConstant.IS_ROLE_CLINIC_STAFF + " or "
            + KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<MessageResponse>> hideOwner(@PathVariable String uuid,
            @PathVariable String ownerUuid) {
        clinicService.hideOwner(uuid, ownerUuid, email());
        return success(new MessageResponse("Client hidden from clinic lists (records kept)"));
    }

    @GetMapping(ApiUrl.CLINIC_BOOKINGS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<PaginationModel<BookingModel>>> bookings(@PathVariable String uuid,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return success(clinicService.bookings(uuid, status, page, size, email()));
    }

    @GetMapping(ApiUrl.CLINIC_RETENTION_ALERTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<RetentionAlertModel>>> retentionAlerts(@PathVariable String uuid) {
        return success(clinicService.retentionAlerts(uuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_RETENTION_ALERT_NOTIFY)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<Void>> notifyAlert(@PathVariable String uuid, @PathVariable String alertId) {
        clinicService.notifyAlert(uuid, alertId, email());
        return success(null);
    }

    @GetMapping(ApiUrl.CLINIC_PATIENT_HEALTH_EVENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<HealthEventModel>>> healthEvents(@PathVariable String uuid,
            @PathVariable String petUuid) {
        return success(clinicService.healthEvents(uuid, petUuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_PATIENT_HEALTH_EVENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<HealthEventModel>> createHealthEvent(@PathVariable String uuid,
            @PathVariable String petUuid, @RequestBody @Valid HealthEventRequest request) {
        return success(clinicService.createHealthEvent(uuid, petUuid, request, email()));
    }

    @PostMapping(ApiUrl.CLINIC_SHUTDOWN)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> shutdown(@PathVariable String uuid) {
        return success(clinicService.shutdown(uuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_REOPEN)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> reopen(@PathVariable String uuid) {
        return success(clinicService.reopen(uuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_STATS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicStatsModel>> stats(@PathVariable String uuid) {
        return success(clinicService.stats(uuid, email()));
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
