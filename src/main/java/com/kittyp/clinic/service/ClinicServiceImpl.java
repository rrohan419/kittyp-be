package com.kittyp.clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.booking.dao.BookingDao;
import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.AddOwnerPetRequest;
import com.kittyp.clinic.dto.ClinicDtos.AddPatientRequest;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicDoctorDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicDoctorPatientModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicOwnerModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicOwnerPetModel;
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
import com.kittyp.clinic.dto.ClinicDtos.InvoiceSummaryModel;
import com.kittyp.clinic.dto.ClinicDtos.OwnerSummaryModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientPetModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.clinic.dto.ClinicDtos.VaccineScheduleModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.entity.ClinicPatientPet;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPatientPetRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.health.entity.HealthEvent;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Address;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.vaccine.dao.PetVaccineScheduleDao;
import com.kittyp.vaccine.entity.PetVaccineSchedule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicServiceImpl implements ClinicService {

    private static final ERole CLINIC_ADMIN_ROLE = ERole.ROLE_CLINIC_ADMIN;

    private final ClinicDao clinicDao;
    private final ClinicStaffDao clinicStaffDao;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicDoctorInviteRepository clinicDoctorInviteRepository;
    private final ClinicPetOwnerRepository clinicPetOwnerRepository;
    private final ClinicPatientPetRepository clinicPatientPetRepository;
    private final PetsRepository petsRepository;
    private final DoctorProfileDao doctorProfileDao;
    private final BookingDao bookingDao;
    private final HealthEventDao healthEventDao;
    private final PetVaccineScheduleDao petVaccineScheduleDao;
    private final NotificationLogRepository notificationLogRepository;
    private final UserDao userDao;
    private final PetDao petDao;
    private final ZeptoMailService zeptoMailService;
    private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
    private final ConsultationInvoiceRepository consultationInvoiceRepository;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Override
    public List<ClinicModel> mine(String email) {
        User user = userDao.userByEmail(email);
        Map<Long, Clinic> clinics = new HashMap<>();
        clinicDao.findAllByOwnerUserId(user.getId()).forEach(clinic -> clinics.put(clinic.getId(), clinic));
        clinicStaffDao.findActiveByUserId(user.getId()).forEach(staff -> clinics.put(staff.getClinic().getId(), staff.getClinic()));
        clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(user.getId())
                .forEach(affiliation -> clinics.put(affiliation.getClinic().getId(), affiliation.getClinic()));
        return clinics.values().stream().map(this::clinicModel).toList();
    }

    @Override
    @Transactional
    public ClinicModel create(ClinicRequest request, String email) {
        User owner = userDao.userByEmail(email);
        Clinic clinic = Clinic.builder().uuid(UUID.randomUUID().toString()).name(request.name())
                .licenseNumber(request.licenseNumber()).address(request.address()).phone(request.phone()).email(request.email())
                .timezone(request.timezone()).operatingHours(request.operatingHours()).owner(owner).status(ClinicStatus.PENDING)
                .build();
        clinic = clinicDao.saveClinic(clinic);

        DoctorProfile doctorProfile = doctorProfileDao.findByUserId(owner.getId());
        if (doctorProfile != null) {
            ClinicDoctor affiliation = ClinicDoctor.builder().clinic(clinic).doctor(doctorProfile).role("owner")
                    .isActive(true).joinedAt(LocalDate.now()).build();
            clinicDoctorRepository.save(affiliation);
        }

        return clinicModel(clinic);
    }

    @Override
    public ClinicModel get(String clinicUuid, String email) {
        return clinicModel(access(clinicUuid, email));
    }

    @Override
    @Transactional
    public ClinicModel update(String clinicUuid, ClinicRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        clinic.setName(request.name());
        clinic.setAddress(request.address());
        clinic.setPhone(request.phone());
        clinic.setTimezone(request.timezone());
        clinic.setOperatingHours(request.operatingHours());
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    public ClinicModel switchClinic(String clinicUuid, String email) {
        return clinicModel(access(clinicUuid, email));
    }

    @Override
    @Transactional
    public ClinicModel shutdown(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        if (clinic.getStatus() == ClinicStatus.SHUTDOWN) {
            return clinicModel(clinic);
        }
        clinic.setStatus(ClinicStatus.SHUTDOWN);
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    @Transactional
    public ClinicModel reopen(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        if (clinic.getStatus() != ClinicStatus.SHUTDOWN) {
            return clinicModel(clinic);
        }
        clinic.setStatus(ClinicStatus.VERIFIED);
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    public ClinicStatsModel stats(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        long diagnosed = healthEventDao.countDistinctPetsByClinic(clinic.getId());
        long patients = patientMap(clinic).size();
        return new ClinicStatsModel(diagnosed, patients);
    }

    @Override
    public List<DoctorModel> doctors(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        return clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).stream().map(this::doctorModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicDoctorDetailModel doctorDetail(String clinicUuid, String doctorUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        ClinicDoctor affiliation = clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(clinic.getId(), doctorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("doctor", "uuid", doctorUuid));
        DoctorProfile doctor = affiliation.getDoctor();
        User user = doctor.getUser();

        Map<String, List<Booking>> bookingsByPet = bookingDao.findByClinic(clinic.getId()).stream()
                .filter(b -> b.getDoctor() != null && doctor.getId().equals(b.getDoctor().getId()))
                .filter(b -> b.getPet() != null)
                .collect(Collectors.groupingBy(b -> b.getPet().getUuid()));

        Map<String, ClinicDoctorPatientModel> patients = new HashMap<>();

        for (Map.Entry<String, List<Booking>> entry : bookingsByPet.entrySet()) {
            Pet pet = entry.getValue().get(0).getPet();
            if (pet.getClinic() == null || !clinic.getId().equals(pet.getClinic().getId()) || pet.getClinicOwner() == null) {
                continue;
            }
            LocalDateTime lastAppt = entry.getValue().stream()
                    .map(Booking::getSlotStart)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            patients.put(pet.getUuid(), new ClinicDoctorPatientModel(toPetListModel(pet), ownerSummary(pet.getClinicOwner()),
                    entry.getValue().size(), lastAppt));
        }

        // Also include clinic pets whose owner account is this doctor (same linked user).
        Long doctorUserId = user.getId();
        for (Pet pet : petsRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            if (pet.getClinicOwner() == null || pet.getClinicOwner().getLinkedUser() == null) {
                continue;
            }
            if (!doctorUserId.equals(pet.getClinicOwner().getLinkedUser().getId())) {
                continue;
            }
            patients.computeIfAbsent(pet.getUuid(),
                    id -> new ClinicDoctorPatientModel(toPetListModel(pet), ownerSummary(pet.getClinicOwner()), 0, null));
        }

        List<ClinicDoctorPatientModel> patientList = patients.values().stream()
                .sorted(Comparator
                        .comparing(ClinicDoctorPatientModel::lastAppointment, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(p -> p.pet().name(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new ClinicDoctorDetailModel(
                doctor.getUuid(),
                user.getUuid(),
                fullName(user),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                doctor.getPhoneNumber() != null ? doctor.getPhoneNumber() : user.getPhoneNumber(),
                doctor.getSpecialization() == null ? null : doctor.getSpecialization().name(),
                doctor.getRegistrationNumber(),
                doctor.getLicenseNumber(),
                doctor.getBio(),
                doctor.getPhotoUrl(),
                doctor.getExperienceYears(),
                affiliation.getRole(),
                affiliation.getIsActive(),
                affiliation.getJoinedAt() == null ? null : affiliation.getJoinedAt().toString(),
                doctor.getStatus() == null ? null : doctor.getStatus().name(),
                doctor.getDegreeCertificateUrl(),
                doctor.getRegistrationCertificateUrl(),
                doctor.getGovernmentIdUrl(),
                doctor.getLicenseDocumentUrl(),
                doctor.getClinicPhotosUrls(),
                doctor.isEmailOtpVerified(),
                doctor.isPhoneOtpVerified(),
                doctor.isCheckMobileOtp(),
                doctor.isCheckEmailOtp(),
                doctor.isCheckGovernmentId(),
                doctor.isCheckDegree(),
                doctor.isCheckRegistrationCertificate(),
                doctor.isCheckClinicAddress(),
                doctor.isCheckRegistrationNumber(),
                doctor.isCheckGoogleMapsMatch(),
                doctor.isCheckClinicPhotos(),
                doctor.getSubmittedAt() == null ? null : doctor.getSubmittedAt().toString(),
                doctor.getReviewedAt() == null ? null : doctor.getReviewedAt().toString(),
                doctor.getReviewNotes(),
                patientList);
    }

    @Override
    @Transactional
    public DoctorInviteModel inviteDoctor(String clinicUuid, DoctorInviteRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        User inviter = userDao.userByEmail(email);
        requireClinicManager(clinic, inviter);

        long invitesLastHour = clinicDoctorInviteRepository.countByClinic_IdAndCreatedAtAfter(
                clinic.getId(), LocalDateTime.now().minusHours(1));
        if (invitesLastHour >= 20) {
            throw new CustomException("Too many doctor invites sent recently. Try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasDoctorUuid = request.doctorUuid() != null && !request.doctorUuid().isBlank();
        if (hasEmail == hasDoctorUuid) {
            throw new CustomException("Provide either doctor email or doctor ID (not both)", HttpStatus.BAD_REQUEST);
        }

        String inviteEmail;
        String doctorName;

        if (hasDoctorUuid) {
            DoctorProfile profile = doctorProfileDao.findByUuid(request.doctorUuid().trim());
            DoctorLookupModel lookup = new DoctorLookupModel(profile.getUuid(), fullName(profile.getUser()),
                    profile.getUser().getEmail());
            boolean isDoctor = profile.getUser().getUserRoles().stream()
                    .anyMatch(ur -> ERole.ROLE_DOCTOR.equals(ur.getRole().getName()));
            if (!isDoctor) {
                throw new CustomException("User is not a doctor on KittyP", HttpStatus.BAD_REQUEST);
            }
            inviteEmail = lookup.email().toLowerCase();
            doctorName = request.name() != null && !request.name().isBlank() ? request.name().trim() : lookup.name();
            if (inviteEmail.equalsIgnoreCase(inviter.getEmail())) {
                throw new CustomException("You cannot invite your own account to this clinic", HttpStatus.BAD_REQUEST);
            }
            if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(),
                    profile.getUser().getId())) {
                throw new CustomException(
                        "This doctor is already on your clinic roster — open Doctors to see them. No new invite is needed.",
                        HttpStatus.CONFLICT);
            }
        } else {
            inviteEmail = request.email().trim().toLowerCase();
            if (!inviteEmail.contains("@") || inviteEmail.length() < 5) {
                throw new CustomException("Enter a valid doctor email", HttpStatus.BAD_REQUEST);
            }
            if (request.name() == null || request.name().isBlank()) {
                throw new CustomException("Doctor name is required when inviting by email", HttpStatus.BAD_REQUEST);
            }
            doctorName = request.name().trim();
            if (inviteEmail.equalsIgnoreCase(inviter.getEmail())) {
                throw new CustomException("You cannot invite your own account to this clinic", HttpStatus.BAD_REQUEST);
            }
            if (userDao.userPresentByEmail(inviteEmail)) {
                User existingUser = userDao.userByEmail(inviteEmail);
                if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(),
                        existingUser.getId())) {
                    throw new CustomException(
                            "This doctor is already on your clinic roster — open Doctors to see them. No new invite is needed.",
                            HttpStatus.CONFLICT);
                }
            }
        }

        // Refresh an existing pending invite (same clinic + email) instead of failing.
        ClinicDoctorInvite invite = clinicDoctorInviteRepository
                .findByClinic_IdAndEmailIgnoreCaseAndStatus(clinic.getId(), inviteEmail, ClinicDoctorInviteStatus.PENDING)
                .orElse(null);

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        if (invite == null) {
            invite = ClinicDoctorInvite.builder()
                    .uuid(UUID.randomUUID().toString())
                    .clinic(clinic)
                    .email(inviteEmail)
                    .doctorName(doctorName)
                    .token(token)
                    .status(ClinicDoctorInviteStatus.PENDING)
                    .invitedByUserId(inviter.getId())
                    .expiresAt(expiresAt)
                    .build();
        } else {
            invite.setDoctorName(doctorName);
            invite.setToken(token);
            invite.setExpiresAt(expiresAt);
            invite.setInvitedByUserId(inviter.getId());
        }
        invite = clinicDoctorInviteRepository.save(invite);

        String acceptUrl = frontendBaseUrl.replaceAll("/$", "") + "/clinic-invite/accept?token=" + invite.getToken();
        // Email may fail locally (missing Zepto template); invite is still stored and returned with token.
        zeptoMailService.sendClinicDoctorInviteEmail(inviteEmail, doctorName, clinic.getName(), acceptUrl);

        // Include token so the clinic UI can show a shareable accept link when email is delayed.
        return inviteModel(invite, true);
    }

    @Override
    public DoctorLookupModel lookupDoctor(String doctorUuid, String email) {
        User caller = userDao.userByEmail(email);
        DoctorLookupModel lookup = resolveDoctorLookup(doctorUuid);
        DoctorProfile profile = doctorProfileDao.findByUuid(doctorUuid.trim());
        User doctorUser = profile.getUser();

        boolean canInviteDoctors = caller.getUserRoles().stream().anyMatch(ur -> {
            ERole role = ur.getRole().getName();
            return ERole.ROLE_CLINIC_ADMIN.equals(role) || ERole.ROLE_CLINIC_STAFF.equals(role)
                    || ERole.ROLE_ADMIN.equals(role);
        }) || !clinicDao.findAllByOwnerUserId(caller.getId()).isEmpty();

        if (canInviteDoctors) {
            return lookup;
        }

        // Otherwise only reveal doctor email/name if they share at least one clinic with the caller.
        Set<Long> callerClinicIds = new HashSet<>();
        clinicDao.findAllByOwnerUserId(caller.getId()).forEach(c -> callerClinicIds.add(c.getId()));
        clinicStaffDao.findActiveByUserId(caller.getId()).forEach(s -> callerClinicIds.add(s.getClinic().getId()));
        clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(caller.getId())
                .forEach(a -> callerClinicIds.add(a.getClinic().getId()));

        boolean shared = clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(doctorUser.getId()).stream()
                .anyMatch(a -> callerClinicIds.contains(a.getClinic().getId()));
        if (!shared) {
            // 404 avoids confirming whether a doctor UUID exists outside shared clinics.
            throw new ResourceNotFoundException("Doctor", "uuid", doctorUuid);
        }
        return lookup;
    }

    private DoctorLookupModel resolveDoctorLookup(String doctorUuid) {
        if (doctorUuid == null || doctorUuid.isBlank()) {
            throw new CustomException("Doctor ID is required", HttpStatus.BAD_REQUEST);
        }
        DoctorProfile profile = doctorProfileDao.findByUuid(doctorUuid.trim());
        User doctorUser = profile.getUser();
        boolean isDoctor = doctorUser.getUserRoles().stream()
                .anyMatch(ur -> ERole.ROLE_DOCTOR.equals(ur.getRole().getName()));
        if (!isDoctor) {
            throw new CustomException("User is not a doctor on KittyP", HttpStatus.BAD_REQUEST);
        }
        return new DoctorLookupModel(profile.getUuid(), fullName(doctorUser), doctorUser.getEmail());
    }

    @Override
    @Transactional
    public List<DoctorInviteModel> listDoctorInvites(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        return clinicDoctorInviteRepository.findByClinic_IdAndStatus(clinic.getId(), ClinicDoctorInviteStatus.PENDING)
                .stream()
                .map(invite -> {
                    if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
                        invite.setStatus(ClinicDoctorInviteStatus.EXPIRED);
                        clinicDoctorInviteRepository.save(invite);
                    }
                    return inviteModel(invite);
                })
                .filter(m -> ClinicDoctorInviteStatus.PENDING.name().equals(m.status()))
                .toList();
    }

    @Override
    @Transactional
    public List<DoctorInviteModel> listMyPendingInvites(String email) {
        User user = userDao.userByEmail(email);
        return clinicDoctorInviteRepository
                .findByEmailIgnoreCaseAndStatus(user.getEmail(), ClinicDoctorInviteStatus.PENDING)
                .stream()
                .map(invite -> {
                    if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
                        invite.setStatus(ClinicDoctorInviteStatus.EXPIRED);
                        clinicDoctorInviteRepository.save(invite);
                        return null;
                    }
                    // Include token so the doctor can open/accept from the notification inbox.
                    return inviteModel(invite, true);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public void revokeDoctorInvite(String clinicUuid, String inviteUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        ClinicDoctorInvite invite = clinicDoctorInviteRepository.findByUuid(inviteUuid)
                .orElseThrow(() -> new ResourceNotFoundException("invite", "uuid", inviteUuid));
        if (!invite.getClinic().getId().equals(clinic.getId())) {
            throw new AccessDeniedException("Invite does not belong to this clinic");
        }
        if (invite.getStatus() != ClinicDoctorInviteStatus.PENDING) {
            throw new CustomException("Only pending invites can be revoked", HttpStatus.BAD_REQUEST);
        }
        invite.setStatus(ClinicDoctorInviteStatus.REVOKED);
        clinicDoctorInviteRepository.save(invite);
    }

    @Override
    public DoctorInvitePreview previewInvite(String token) {
        ClinicDoctorInvite invite = requireInviteByToken(token);
        boolean expired = invite.getExpiresAt().isBefore(LocalDateTime.now())
                || invite.getStatus() == ClinicDoctorInviteStatus.EXPIRED;
        boolean accepted = invite.getStatus() == ClinicDoctorInviteStatus.ACCEPTED;
        return new DoctorInvitePreview(invite.getClinic().getName(), invite.getDoctorName(),
                maskEmail(invite.getEmail()), expired, accepted, invite.getStatus().name());
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return null;
        }
        String[] parts = email.trim().split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.length() <= 1 ? "*" : local.charAt(0) + "***";
        return maskedLocal + "@" + domain;
    }

    @Override
    @Transactional
    public DoctorModel acceptInvite(String token, String email) {
        ClinicDoctorInvite invite = requireInviteByToken(token);
        if (invite.getStatus() != ClinicDoctorInviteStatus.PENDING) {
            throw new CustomException("This invite is no longer valid", HttpStatus.BAD_REQUEST);
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(ClinicDoctorInviteStatus.EXPIRED);
            clinicDoctorInviteRepository.save(invite);
            throw new CustomException("This invite has expired", HttpStatus.BAD_REQUEST);
        }

        User user = userDao.userByEmail(email);
        if (user == null || !invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new CustomException("Sign in with the invited email (" + invite.getEmail() + ") to accept",
                    HttpStatus.FORBIDDEN);
        }

        boolean isDoctor = user.getUserRoles().stream()
                .anyMatch(ur -> ERole.ROLE_DOCTOR.equals(ur.getRole().getName()));
        if (!isDoctor) {
            throw new CustomException("Only doctor accounts can accept clinic invitations", HttpStatus.FORBIDDEN);
        }

        Clinic clinic = invite.getClinic();
        requireOperational(clinic);

        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            profile = doctorProfileDao.save(DoctorProfile.builder()
                    .uuid(UUID.randomUUID().toString())
                    .user(user)
                    .phoneNumber(user.getPhoneNumber())
                    .status(DoctorStatus.REGISTERED)
                    .build());
        }

        if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), user.getId())) {
            invite.setStatus(ClinicDoctorInviteStatus.ACCEPTED);
            clinicDoctorInviteRepository.save(invite);
            return clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).stream()
                    .filter(a -> a.getDoctor().getUser().getId().equals(user.getId()))
                    .findFirst()
                    .map(this::doctorModel)
                    .orElseThrow();
        }

        ClinicDoctor affiliation = ClinicDoctor.builder()
                .clinic(clinic)
                .doctor(profile)
                .role("doctor")
                .isActive(true)
                .joinedAt(LocalDate.now())
                .build();
        affiliation = clinicDoctorRepository.save(affiliation);

        invite.setStatus(ClinicDoctorInviteStatus.ACCEPTED);
        clinicDoctorInviteRepository.save(invite);

        return doctorModel(affiliation);
    }

    private ClinicDoctorInvite requireInviteByToken(String token) {
        return clinicDoctorInviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("invite", "token", token));
    }

    private DoctorInviteModel inviteModel(ClinicDoctorInvite invite) {
        return inviteModel(invite, false);
    }

    private DoctorInviteModel inviteModel(ClinicDoctorInvite invite, boolean includeToken) {
        return new DoctorInviteModel(invite.getUuid(), invite.getEmail(), invite.getDoctorName(),
                invite.getStatus().name(), invite.getExpiresAt().toString(), invite.getClinic().getUuid(),
                invite.getClinic().getName(), includeToken ? invite.getToken() : null);
    }

    @Override
    @Transactional
    public List<PatientModel> patients(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        ensureDemoClinicPatients(clinic);
        Map<String, PatientModel> merged = new HashMap<>(patientMap(clinic));
        for (Pet pet : petsRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            merged.put(pet.getUuid(), toClinicPatientModel(pet));
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(PatientModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public PatientDetailModel patientDetail(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);

        Optional<Pet> clinicPetOpt = petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(petUuid, clinic.getId());
        if (clinicPetOpt.isPresent() && clinicPetOpt.get().getClinicOwner() != null) {
            return clinicRegisteredPatientDetail(clinic, clinicPetOpt.get());
        }

        Map<String, PatientModel> clinicPatients = patientMap(clinic);

        PatientModel patient = clinicPatients.get(petUuid);
        if (patient == null) {
            throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
        }
        User owner = userDao.userByPetUuid(petUuid);

        OwnerSummaryModel ownerSummary = new OwnerSummaryModel(owner.getUuid(), fullName(owner), owner.getEmail(),
                formatPhone(owner), primaryAddress(owner));

        // Only expose pets that have a relationship with this clinic (no cross-pet PHI leak).
        Set<String> clinicPetUuids = clinicPatients.keySet();
        List<Pet> ownedPets = owner.getPets() == null ? List.of() : owner.getPets();
        List<PatientPetModel> pets = ownedPets.stream()
                .filter(pet -> clinicPetUuids.contains(pet.getUuid()))
                .sorted(Comparator.comparing(Pet::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(pet -> {
                    PatientModel clinicPatient = clinicPatients.get(pet.getUuid());
                    return new PatientPetModel(pet.getUuid(), pet.getName(), pet.getType(), pet.getBreed(),
                            clinicPatient != null ? clinicPatient.lastVisit() : null, true);
                })
                .toList();

        List<HealthEventModel> events = healthEventsFor(clinic.getId(), petUuid);
        List<VaccineScheduleModel> vaccines = petVaccineScheduleDao.findByPetUuid(petUuid).stream()
                .map(this::vaccineModel)
                .toList();

        return new PatientDetailModel(patient, ownerSummary, pets, events, vaccines);
    }

    @Override
    @Transactional
    public PatientDetailModel addPatient(String clinicUuid, AddPatientRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        requireClinicManager(clinic, userDao.userByEmail(email));

        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(request.ownerEmail());
        String phoneDigits = ClinicOwnerUserLinkService.normalizePhoneDigits(request.ownerPhone());
        if (phoneDigits == null || !phoneDigits.matches("\\d{10}")) {
            throw new CustomException("Owner phone must be a valid 10-digit number", HttpStatus.BAD_REQUEST);
        }

        ClinicPetOwner owner = clinicPetOwnerRepository
                .findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(clinic.getId(), ownerEmail)
                .orElse(null);

        if (owner == null) {
            owner = ClinicPetOwner.builder()
                    .uuid(UUID.randomUUID().toString())
                    .clinic(clinic)
                    .firstName(request.ownerFirstName().trim())
                    .lastName(request.ownerLastName() == null ? "" : request.ownerLastName().trim())
                    .email(ownerEmail)
                    .phone(phoneDigits)
                    .alternatePhone(blankToNull(request.ownerAlternatePhone()) == null ? null
                            : ClinicOwnerUserLinkService.normalizePhoneDigits(request.ownerAlternatePhone()))
                    .address(request.ownerAddress() == null || request.ownerAddress().isBlank()
                            ? null
                            : request.ownerAddress().trim())
                    .notes(blankToNull(request.ownerNotes()))
                    .build();
        } else {
            owner.setFirstName(request.ownerFirstName().trim());
            if (request.ownerLastName() != null) {
                owner.setLastName(request.ownerLastName().trim());
            }
            owner.setPhone(phoneDigits);
            if (request.ownerAlternatePhone() != null && !request.ownerAlternatePhone().isBlank()) {
                owner.setAlternatePhone(ClinicOwnerUserLinkService.normalizePhoneDigits(request.ownerAlternatePhone()));
            }
            if (request.ownerAddress() != null && !request.ownerAddress().isBlank()) {
                owner.setAddress(request.ownerAddress().trim());
            }
            if (request.ownerNotes() != null) {
                owner.setNotes(blankToNull(request.ownerNotes()));
            }
        }
        owner = clinicPetOwnerRepository.save(owner);
        owner = clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);

        Pet pet = saveClinicPet(clinic, owner, request.petName().trim(), blankToNull(request.petType()),
                blankToNull(request.petBreed()), blankToNull(request.petGender()), request.petDateOfBirth(),
                blankToNull(request.petWeight()), blankToNull(request.petMicrochipNumber()), null, null);
        if (owner.getLinkedUser() != null) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
        }

        return clinicRegisteredPatientDetail(clinic, pet);
    }

    @Override
    @Transactional
    public List<ClinicOwnerModel> listOwners(String clinicUuid, String q, String email) {
        Clinic clinic = access(clinicUuid, email);
        ensureDemoClinicPatients(clinic);
        List<ClinicPetOwner> owners = (q == null || q.isBlank())
                ? clinicPetOwnerRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())
                : clinicPetOwnerRepository.searchByClinic(clinic.getId(), q.trim());
        return owners.stream()
                .map(this::toOwnerModel)
                .sorted(Comparator.comparing(ClinicOwnerModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional
    public ClinicOwnerModel createOwner(String clinicUuid, CreateOwnerRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        requireClinicManager(clinic, userDao.userByEmail(email));

        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(request.email());
        String phoneDigits = ClinicOwnerUserLinkService.normalizePhoneDigits(request.phone());
        if (phoneDigits == null || !phoneDigits.matches("\\d{10}")) {
            throw new CustomException("Owner phone must be a valid 10-digit number", HttpStatus.BAD_REQUEST);
        }
        if (clinicPetOwnerRepository.findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(clinic.getId(), ownerEmail)
                .isPresent()) {
            throw new CustomException("An owner with this email already exists at this clinic", HttpStatus.CONFLICT);
        }

        ClinicPetOwner owner = ClinicPetOwner.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(clinic)
                .firstName(request.firstName().trim())
                .lastName(request.lastName() == null ? "" : request.lastName().trim())
                .email(ownerEmail)
                .phone(phoneDigits)
                .alternatePhone(blankToNull(request.alternatePhone()) == null ? null
                        : ClinicOwnerUserLinkService.normalizePhoneDigits(request.alternatePhone()))
                .address(blankToNull(request.address()))
                .notes(blankToNull(request.notes()))
                .build();
        owner = clinicPetOwnerRepository.save(owner);
        owner = clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
        return toOwnerModel(owner);
    }

    @Override
    public ClinicOwnerProfileModel ownerProfile(String clinicUuid, String ownerUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        ClinicPetOwner owner = clinicPetOwnerRepository.findByUuidAndClinic_IdAndIsActiveTrue(ownerUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("owner", "uuid", ownerUuid));
        ClinicOwnerModel model = toOwnerModel(owner);
        long invoiceCount = 0;
        String billingStatus = "NONE";
        if (owner.getLinkedUser() != null) {
            invoiceCount = consultationInvoiceRepository.countByOwner_IdAndClinic_Id(owner.getLinkedUser().getId(),
                    clinic.getId());
            billingStatus = invoiceCount > 0 ? "HAS_INVOICES" : "LINKED_NO_INVOICES";
        } else {
            billingStatus = "UNLINKED";
        }
        return new ClinicOwnerProfileModel(model, billingStatus, invoiceCount);
    }

    @Override
    @Transactional
    public ClinicPetListModel addPetToOwner(String clinicUuid, String ownerUuid, AddOwnerPetRequest request,
            String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        requireClinicManager(clinic, userDao.userByEmail(email));

        ClinicPetOwner owner = clinicPetOwnerRepository.findByUuidAndClinic_IdAndIsActiveTrue(ownerUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("owner", "uuid", ownerUuid));

        Pet pet = saveClinicPet(clinic, owner, request.name().trim(), blankToNull(request.species()),
                blankToNull(request.breed()), blankToNull(request.gender()), request.dateOfBirth(),
                blankToNull(request.weight()), blankToNull(request.microchipNumber()), blankToNull(request.photoUrl()),
                blankToNull(request.patientNumber()));
        if (owner.getLinkedUser() != null) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
        }
        return toPetListModel(pet);
    }

    @Override
    @Transactional
    public List<ClinicPetListModel> listPets(String clinicUuid, String q, String email) {
        Clinic clinic = access(clinicUuid, email);
        ensureDemoClinicPatients(clinic);
        List<Pet> pets = (q == null || q.isBlank())
                ? petsRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())
                : petsRepository.searchByClinic(clinic.getId(), q.trim());
        return pets.stream()
                .map(this::toPetListModel)
                .sorted(Comparator.comparing(ClinicPetListModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public ClinicPetMedicalProfileModel petMedicalProfile(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        Pet pet = petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(petUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("pet", "uuid", petUuid));
        if (pet.getClinicOwner() == null) {
            throw new ResourceNotFoundException("pet", "uuid", petUuid);
        }

        ClinicPetOwner owner = pet.getClinicOwner();
        ClinicPetListModel petModel = toPetListModel(pet);
        OwnerSummaryModel ownerSummary = ownerSummary(owner);

        String medicalPetKey = pet.getUuid();
        List<HealthEventModel> timeline = healthEventsFor(clinic.getId(), medicalPetKey);
        List<VaccineScheduleModel> vaccines = petVaccineScheduleDao.findByPetUuid(medicalPetKey).stream()
                .map(this::vaccineModel).toList();

        List<BookingModel> appointments = bookingDao.findByClinic(clinic.getId()).stream()
                .filter(b -> b.getPet() != null && medicalPetKey.equals(b.getPet().getUuid()))
                .sorted(Comparator.comparing(Booking::getSlotStart, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::bookingModel)
                .toList();

        List<InvoiceSummaryModel> invoices = List.of();
        if (owner.getLinkedUser() != null) {
            invoices = consultationInvoiceRepository
                    .findAllByOwner_IdAndClinic_IdOrderByCreatedAtDesc(owner.getLinkedUser().getId(), clinic.getId())
                    .stream()
                    .filter(inv -> inv.getPetUuid() == null || medicalPetKey.equals(inv.getPetUuid()))
                    .map(this::invoiceSummary)
                    .toList();
        }

        return new ClinicPetMedicalProfileModel(petModel, ownerSummary, timeline, appointments, vaccines,
                List.of(), List.of(), List.of(), invoices);
    }

    private InvoiceSummaryModel invoiceSummary(ConsultationInvoice inv) {
        return new InvoiceSummaryModel(inv.getUuid(), inv.getStatus() == null ? null : inv.getStatus().name(),
                inv.getAmount() == null ? null : inv.getAmount().toPlainString(), inv.getCurrency(), inv.getPetUuid(),
                inv.getCreatedAt());
    }

    private Pet saveClinicPet(Clinic clinic, ClinicPetOwner owner, String name, String species, String breed,
            String gender, LocalDate dob, String weight, String microchip, String photoUrl, String patientNumber) {
        Pet pet = Pet.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(clinic)
                .clinicOwner(owner)
                .name(name)
                .type(species)
                .breed(breed)
                .gender(gender)
                .dateOfBirth(dob)
                .weight(weight)
                .microchipNumber(microchip)
                .profilePicture(photoUrl)
                .patientNumber(patientNumber)
                .registeredAt(LocalDate.now())
                .isNeutered(false)
                .build();
        return petsRepository.save(pet);
    }

    private ClinicOwnerModel toOwnerModel(ClinicPetOwner owner) {
        List<Pet> pets = petsRepository.findByClinicOwner_IdAndIsActiveTrue(owner.getId());
        LocalDateTime lastVisit = pets.stream()
                .map(Pet::getRegisteredAt)
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .map(LocalDate::atStartOfDay)
                .orElse(null);
        boolean linked = owner.getLinkedUser() != null;
        String linkedUuid = linked ? owner.getLinkedUser().getUuid() : null;
        List<ClinicOwnerPetModel> petModels = pets.stream()
                .sorted(Comparator.comparing(Pet::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(p -> new ClinicOwnerPetModel(p.getUuid(), p.resolveGlobalPetId(), p.getName(), p.getType(),
                        p.getBreed(), p.getGender(), p.getDateOfBirth(), p.getWeight(), p.getMicrochipNumber(),
                        p.getProfilePicture(), p.getPatientNumber(),
                        p.getRegisteredAt() == null ? null : p.getRegisteredAt().atStartOfDay()))
                .toList();
        return new ClinicOwnerModel(owner.getUuid(), clinicOwnerName(owner), owner.getFirstName(), owner.getLastName(),
                owner.getEmail(), formatClinicPhone(owner.getPhone()), formatClinicPhone(owner.getAlternatePhone()),
                owner.getAddress(), owner.getNotes(), linked, linkedUuid, petModels.size(), lastVisit, petModels);
    }

    private ClinicPetListModel toPetListModel(Pet pet) {
        ClinicPetOwner owner = pet.getClinicOwner();
        return new ClinicPetListModel(pet.getUuid(), pet.resolveGlobalPetId(), pet.getName(), pet.getType(),
                pet.getBreed(), pet.getGender(), pet.getDateOfBirth(), pet.getWeight(), pet.getMicrochipNumber(),
                pet.getProfilePicture(), pet.getPatientNumber(), owner.getUuid(), clinicOwnerName(owner),
                formatClinicPhone(owner.getPhone()), owner.getEmail(), owner.getLinkedUser() != null,
                pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay());
    }

    private OwnerSummaryModel ownerSummary(ClinicPetOwner owner) {
        boolean linked = owner.getLinkedUser() != null;
        return new OwnerSummaryModel(owner.getUuid(), clinicOwnerName(owner), owner.getEmail(),
                formatClinicPhone(owner.getPhone()), owner.getAddress(), linked,
                linked ? owner.getLinkedUser().getUuid() : null);
    }

    private PatientDetailModel clinicRegisteredPatientDetail(Clinic clinic, Pet selected) {
        ClinicPetOwner owner = selected.getClinicOwner();
        PatientModel patient = toClinicPatientModel(selected);
        OwnerSummaryModel ownerSummary = ownerSummary(owner);

        List<PatientPetModel> pets = petsRepository.findByClinicOwner_UuidAndIsActiveTrue(owner.getUuid()).stream()
                .filter(p -> p.getClinic() != null && p.getClinic().getId().equals(clinic.getId()))
                .sorted(Comparator.comparing(Pet::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(p -> new PatientPetModel(p.getUuid(), p.getName(), p.getType(), p.getBreed(),
                        p.getRegisteredAt() == null ? null : p.getRegisteredAt().atStartOfDay(), true,
                        p.resolveGlobalPetId(), p.getMicrochipNumber()))
                .toList();

        String medicalKey = selected.getUuid();
        List<HealthEventModel> events = healthEventsFor(clinic.getId(), medicalKey);
        List<VaccineScheduleModel> vaccines = petVaccineScheduleDao.findByPetUuid(medicalKey).stream()
                .map(this::vaccineModel).toList();

        return new PatientDetailModel(patient, ownerSummary, pets, events, vaccines);
    }

    private PatientModel toClinicPatientModel(Pet pet) {
        ClinicPetOwner owner = pet.getClinicOwner();
        LocalDateTime lastVisit = pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay();
        return new PatientModel(pet.getUuid(), pet.getName(), clinicOwnerName(owner), owner.getEmail(), lastVisit,
                owner.getUuid(), formatClinicPhone(owner.getPhone()), owner.getAddress(), pet.getType(),
                pet.getBreed());
    }

    private String clinicOwnerName(ClinicPetOwner owner) {
        String last = owner.getLastName() == null ? "" : owner.getLastName().trim();
        return (owner.getFirstName() + (last.isEmpty() ? "" : " " + last)).trim();
    }

    private String formatClinicPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return "+91 " + digits;
        }
        return phone;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * One-time move of legacy {@code clinic_patient_pets} rows into shared {@code pets}.
     */
    private void migrateLegacyClinicPets(Clinic clinic) {
        List<ClinicPatientPet> legacy = clinicPatientPetRepository.findByClinic_IdAndIsActiveTrue(clinic.getId());
        for (ClinicPatientPet row : legacy) {
            String globalId = row.resolveGlobalPetId();
            Pet existing = petsRepository.findByUuid(globalId);
            if (existing == null) {
                existing = petsRepository.findByUuid(row.getUuid());
            }
            if (existing != null) {
                if (existing.getClinicOwner() == null) {
                    existing.setClinicOwner(row.getOwner());
                    existing.setClinic(clinic);
                    if (existing.getRegisteredAt() == null) {
                        existing.setRegisteredAt(row.getRegisteredAt());
                    }
                    if (existing.getMicrochipNumber() == null) {
                        existing.setMicrochipNumber(row.getMicrochipNumber());
                    }
                    petsRepository.save(existing);
                }
                continue;
            }
            Pet pet = Pet.builder()
                    .uuid(globalId)
                    .clinic(clinic)
                    .clinicOwner(row.getOwner())
                    .name(row.getName())
                    .type(row.getSpecies())
                    .breed(row.getBreed())
                    .gender(row.getGender())
                    .dateOfBirth(row.getDateOfBirth())
                    .weight(row.getWeight())
                    .microchipNumber(row.getMicrochipNumber())
                    .profilePicture(row.getPhotoUrl())
                    .patientNumber(row.getPatientNumber())
                    .registeredAt(row.getRegisteredAt() != null ? row.getRegisteredAt() : LocalDate.now())
                    .isNeutered(false)
                    .build();
            petsRepository.save(pet);
        }
    }

    @Transactional
    protected void ensureDemoClinicPatients(Clinic clinic) {
        migrateLegacyClinicPets(clinic);
        if (petsRepository.countByClinic_IdAndIsActiveTrue(clinic.getId()) > 0
                || clinicPetOwnerRepository.countByClinic_IdAndIsActiveTrue(clinic.getId()) > 0) {
            return;
        }
        String demoSuffix = clinic.getUuid().substring(0, Math.min(8, clinic.getUuid().length()));
        ClinicPetOwner demoOwner = clinicPetOwnerRepository.save(ClinicPetOwner.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(clinic)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya.demo@" + demoSuffix + ".kittyp.local")
                .phone("9876543210")
                .address("12 MG Road, Pune")
                .notes("Demo client — not linked to a KittyP account")
                .build());
        Pet bruno = saveClinicPet(clinic, demoOwner, "Bruno", "Dog", "Labrador", "Male", LocalDate.now().minusYears(3),
                "28", null,
                "https://images.unsplash.com/photo-1552053831-71594a27632d?w=600&h=400&fit=crop", null);
        bruno.setRegisteredAt(LocalDate.now().minusDays(14));
        petsRepository.save(bruno);
        Pet mochi = saveClinicPet(clinic, demoOwner, "Mochi", "Cat", "Persian", "Female", LocalDate.now().minusYears(2),
                "4.2", null,
                "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600&h=400&fit=crop", null);
        mochi.setRegisteredAt(LocalDate.now().minusDays(7));
        petsRepository.save(mochi);

        ClinicPetOwner demoOwner2 = clinicPetOwnerRepository.save(ClinicPetOwner.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(clinic)
                .firstName("Amit")
                .lastName("Patel")
                .email("amit.demo@" + demoSuffix + ".kittyp.local")
                .phone("9123456780")
                .address("45 FC Road, Pune")
                .build());
        Pet coco = saveClinicPet(clinic, demoOwner2, "Coco", "Dog", "Beagle", "Female", LocalDate.now().minusYears(1),
                "12", null,
                "https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?w=600&h=400&fit=crop", null);
        coco.setRegisteredAt(LocalDate.now().minusDays(3));
        petsRepository.save(coco);
    }

    @Override
    public PaginationModel<BookingModel> bookings(String clinicUuid, String status, int page, int size, String email) {
        Clinic clinic = access(clinicUuid, email);
        BookingStatus bookingStatus = status == null || status.isBlank() ? null : BookingStatus.valueOf(status.toUpperCase());
        Page<Booking> bookings = bookingDao.findByClinic(clinic.getId(), bookingStatus, PageRequest.of(page, size));
        return PaginationModel.<BookingModel>builder().models(bookings.map(this::bookingModel).toList())
                .totalPages(bookings.getTotalPages()).totalElements(bookings.getTotalElements()).isFirst(bookings.isFirst())
                .isLast(bookings.isLast()).pageNumber(bookings.getNumber()).pageSize(bookings.getSize()).build();
    }

    @Override
    public List<RetentionAlertModel> retentionAlerts(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        Set<String> patientUuids = patientMap(clinic).keySet();
        LocalDate today = LocalDate.now();
        List<RetentionAlertModel> alerts = petVaccineScheduleDao.findDueOnOrBefore(today.plusDays(90)).stream()
                .filter(schedule -> patientUuids.contains(schedule.getPet().getUuid())).map(schedule -> vaccineAlert(schedule, today))
                .collect(Collectors.toList());

        patientMap(clinic).values().stream().filter(patient -> patient.lastVisit() != null
                && patient.lastVisit().toLocalDate().isBefore(today.minusDays(180))).forEach(patient -> alerts.add(
                        new RetentionAlertModel("lapsed-" + patient.petUuid(), patient.petUuid(), patient.petName(),
                                patient.ownerName(), "LAPSED_VISIT", "No clinic visit in more than 180 days.",
                                ChronoUnit.DAYS.between(patient.lastVisit().toLocalDate(), today), "LAPSED")));
        return alerts;
    }

    @Override
    @Transactional
    public void notifyAlert(String clinicUuid, String alertId, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        RetentionAlertModel alert = retentionAlerts(clinic.getUuid(), email).stream()
                .filter(candidate -> candidate.id().equals(alertId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("retention alert", "id", alertId));
        Pet pet = requirePet(alert.petUuid());
        User owner = userDao.userByPetUuid(pet.getUuid());
        notificationLogRepository.save(NotificationLog.builder().user(owner).pet(pet).type(NotificationType.VACCINATION_DUE)
                .payload(alert.message()).sentAt(LocalDateTime.now()).build());
    }

    @Override
    public List<HealthEventModel> healthEvents(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requirePatient(clinic, petUuid);
        return healthEventsFor(clinic.getId(), petUuid);
    }

    @Override
    @Transactional
    public HealthEventModel createHealthEvent(String clinicUuid, String petUuid, HealthEventRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        requirePatient(clinic, petUuid);
        HealthEvent event = HealthEvent.builder().uuid(UUID.randomUUID().toString()).clinic(clinic).pet(requirePet(petUuid))
                .type(request.type()).title(request.title()).description(request.description()).date(request.date())
                .isPast(request.isPast()).status(request.status()).attachments(request.attachments()).build();
        return healthEventModel(healthEventDao.save(event));
    }

    private Clinic access(String clinicUuid, String email) {
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        User user = userDao.userByEmail(email);
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(user.getId());
        boolean staff = clinicStaffDao.isActiveMember(clinic.getId(), user.getId());
        boolean doctor = clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), user.getId());
        if (!owner && !staff && !doctor) {
            throw new AccessDeniedException("You do not have access to this clinic.");
        }
        return clinic;
    }

    private void requireOperational(Clinic clinic) {
        if (clinic.getStatus() == ClinicStatus.SHUTDOWN) {
            throw new CustomException("This clinic is shut down and is read-only.", HttpStatus.BAD_REQUEST);
        }
    }

    private void requireClinicManager(Clinic clinic, User user) {
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(user.getId());
        if (owner) {
            return;
        }
        // Active staff (clinic admin or staff role) can manage invites/doctors for this clinic.
        if (clinicStaffDao.isActiveMember(clinic.getId(), user.getId())) {
            return;
        }
        boolean clinicAdmin = user.getUserRoles().stream()
                .anyMatch(userRole -> CLINIC_ADMIN_ROLE.equals(userRole.getRole().getName()));
        boolean doctorHere = clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(),
                user.getId());
        if (clinicAdmin && doctorHere) {
            return;
        }
        throw new CustomException("You do not have permission to invite doctors for this clinic",
                HttpStatus.FORBIDDEN);
    }

    private Map<String, PatientModel> patientMap(Clinic clinic) {
        Map<String, LocalDateTime> lastVisits = new HashMap<>();
        List<Booking> bookings = bookingDao.findByClinic(clinic.getId());
        Set<String> petUuids = new HashSet<>();
        bookings.forEach(booking -> {
            petUuids.add(booking.getPet().getUuid());
            lastVisits.merge(booking.getPet().getUuid(), booking.getSlotStart(),
                    (left, right) -> left.isAfter(right) ? left : right);
        });
        healthEventDao.findByClinic(clinic.getId()).forEach(event -> {
            petUuids.add(event.getPet().getUuid());
            if (event.getDate() != null) {
                lastVisits.merge(event.getPet().getUuid(), event.getDate().atStartOfDay(),
                        (left, right) -> left.isAfter(right) ? left : right);
            }
        });
        return petUuids.stream().collect(Collectors.toMap(uuid -> uuid, uuid -> {
            Pet pet = requirePet(uuid);
            User owner = userDao.userByPetUuid(uuid);
            return new PatientModel(uuid, pet.getName(), fullName(owner), owner.getEmail(), lastVisits.get(uuid),
                    owner.getUuid(), formatPhone(owner), primaryAddress(owner), pet.getType(), pet.getBreed());
        }));
    }

    private String formatPhone(User user) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            return null;
        }
        String code = user.getPhoneCountryCode() == null || user.getPhoneCountryCode().isBlank()
                ? "+91"
                : user.getPhoneCountryCode().trim();
        return code + " " + user.getPhoneNumber().trim();
    }

    private String primaryAddress(User user) {
        if (user.getAddresses() == null || user.getAddresses().isEmpty()) {
            return null;
        }
        return user.getAddresses().stream()
                .map(Address::getFormattedAddress)
                .filter(a -> a != null && !a.isBlank())
                .findFirst()
                .orElse(null);
    }

    private PatientModel requirePatient(Clinic clinic, String petUuid) {
        Optional<Pet> clinicPet = petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(petUuid, clinic.getId());
        if (clinicPet.isPresent()) {
            Pet pet = clinicPet.get();
            ClinicPetOwner owner = pet.getClinicOwner();
            return new PatientModel(pet.getUuid(), pet.getName(),
                    owner != null ? clinicOwnerName(owner) : null,
                    owner != null ? owner.getEmail() : null,
                    pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay(),
                    owner != null ? owner.getUuid() : null,
                    owner != null ? formatClinicPhone(owner.getPhone()) : null,
                    owner != null ? owner.getAddress() : null,
                    pet.getType(), pet.getBreed());
        }
        PatientModel patient = patientMap(clinic).get(petUuid);
        if (patient == null) {
            throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
        }
        return patient;
    }

    private Pet requirePet(String petUuid) {
        Pet pet = petDao.petByUuid(petUuid);
        if (pet == null) {
            throw new ResourceNotFoundException("pet", "uuid", petUuid);
        }
        return pet;
    }

    private ClinicModel clinicModel(Clinic clinic) {
        return new ClinicModel(clinic.getUuid(), clinic.getName(), clinic.getLicenseNumber(), clinic.getAddress(),
                clinic.getPhone(), clinic.getEmail(), clinic.getTimezone(), clinic.getOperatingHours(),
                clinic.getStatus().name());
    }

    private DoctorModel doctorModel(ClinicDoctor affiliation) {
        var doctor = affiliation.getDoctor();
        return new DoctorModel(doctor.getUuid(), doctor.getUser().getUuid(), fullName(doctor.getUser()),
                doctor.getUser().getEmail(), doctor.getSpecialization() == null ? null : doctor.getSpecialization().name(),
                affiliation.getRole(), affiliation.getIsActive(),
                doctor.getStatus() == null ? null : doctor.getStatus().name(), doctor.getPhotoUrl());
    }

    private BookingModel bookingModel(Booking booking) {
        return new BookingModel(booking.getUuid(), booking.getPet().getUuid(), booking.getPet().getName(),
                fullName(booking.getOwner()), booking.getDoctor() == null ? null : booking.getDoctor().getUuid(),
                booking.getSlotStart(), booking.getSlotEnd(), booking.getTimezone(), booking.getStatus(),
                booking.getMode().name(), booking.getNotes());
    }

    private List<HealthEventModel> healthEventsFor(Long clinicId, String petUuid) {
        return healthEventDao.findByClinicAndPet(clinicId, petUuid).stream().map(this::healthEventModel).toList();
    }

    private HealthEventModel healthEventModel(HealthEvent event) {
        return new HealthEventModel(event.getUuid(), event.getType().name(), event.getTitle(), event.getDescription(),
                event.getDate(), event.getIsPast(), event.getStatus() == null ? null : event.getStatus().name(),
                event.getAttachments());
    }

    private VaccineScheduleModel vaccineModel(PetVaccineSchedule schedule) {
        return new VaccineScheduleModel(schedule.getId(), schedule.getVaccine().getName(), schedule.getDueDate(),
                schedule.getCompleted(), schedule.getCompletedDate());
    }

    private RetentionAlertModel vaccineAlert(PetVaccineSchedule schedule, LocalDate today) {
        Pet pet = schedule.getPet();
        User owner = userDao.userByPetUuid(pet.getUuid());
        long days = ChronoUnit.DAYS.between(today, schedule.getDueDate());
        String status = days < 0 ? "OVERDUE" : "DUE_SOON";
        String message = days < 0 ? schedule.getVaccine().getName() + " vaccine is overdue."
                : schedule.getVaccine().getName() + " vaccine is due in " + days + " days.";
        return new RetentionAlertModel("vaccine-" + schedule.getId(), pet.getUuid(), pet.getName(), fullName(owner),
                "VACCINE", message, days, status);
    }

    private String fullName(User user) {
        return String.join(" ", user.getFirstName() == null ? "" : user.getFirstName(),
                user.getLastName() == null ? "" : user.getLastName()).trim();
    }
}
