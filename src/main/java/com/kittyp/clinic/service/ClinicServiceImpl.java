package com.kittyp.clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kittyp.booking.dao.BookingDao;
import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.AddVaccineDueRequest;
import com.kittyp.clinic.dto.ClinicDtos.ClinicalRecordModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicalRecordRequest;
import com.kittyp.clinic.dto.ClinicDtos.MarkVaccineGivenRequest;
import com.kittyp.clinic.dto.ClinicDtos.VaccineCatalogModel;
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
import com.kittyp.notification.service.WhatsAppSettingsSupport;
import com.kittyp.clinic.dto.ClinicDtos.DoctorInviteRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorLookupModel;
import com.kittyp.clinic.dto.ClinicDtos.DoctorModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventRequest;
import com.kittyp.clinic.dto.ClinicDtos.InvoiceSummaryModel;
import com.kittyp.clinic.dto.ClinicDtos.OwnerEmailLookupModel;
import com.kittyp.clinic.dto.ClinicDtos.OwnerSummaryModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientPetModel;
import com.kittyp.clinic.dto.ClinicDtos.PlatformUserSearchModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteCompleteRequest;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteModel;
import com.kittyp.clinic.dto.ClinicDtos.StaffInvitePreview;
import com.kittyp.clinic.dto.ClinicDtos.StaffInviteRequest;
import com.kittyp.clinic.dto.ClinicDtos.StaffMemberModel;
import com.kittyp.clinic.dto.ClinicDtos.VaccineScheduleModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.entity.ClinicPatientPet;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.entity.ClinicStaff;
import com.kittyp.clinic.entity.ClinicStaffInvite;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.clinic.enums.ClinicStaffInviteStatus;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorInviteRepository;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPatientPetRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.repository.ClinicStaffInviteRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.util.SafePhotoUrl;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.entity.DoctorPatientEnrollment;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.health.entity.HealthEvent;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.RoleDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Address;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.vaccine.dao.PetVaccineScheduleDao;
import com.kittyp.vaccine.entity.PetVaccineSchedule;
import com.kittyp.vaccine.entity.VaccineMaster;
import com.kittyp.vaccine.repository.VaccineMasterRepository;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.service.ParentBookingEnrollmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private final ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    private final DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;
    private final PetsRepository petsRepository;
    private final DoctorProfileDao doctorProfileDao;
    private final BookingDao bookingDao;
    private final HealthEventDao healthEventDao;
    private final PetVaccineScheduleDao petVaccineScheduleDao;
    private final VaccineMasterRepository vaccineMasterRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final UserDao userDao;
    private final PetDao petDao;
    private final ZeptoMailService zeptoMailService;
    private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
    private final ConsultationInvoiceRepository consultationInvoiceRepository;
    private final VisitDao visitDao;
    private final UserRepository userRepository;
    private final ClinicStaffInviteRepository clinicStaffInviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleDao roleDao;
    private final ParentBookingEnrollmentService parentBookingEnrollmentService;
    private final VerificationCodeService verificationCodeService;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public List<ClinicModel> mine(String email) {
        User user = userDao.userByEmail(email);
        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile != null) {
            ensurePersonalPractice(user, profile);
        }
        Map<Long, Clinic> clinics = new HashMap<>();
        clinicDao.findAllByOwnerUserId(user.getId()).forEach(clinic -> clinics.put(clinic.getId(), clinic));
        clinicStaffDao.findActiveByUserId(user.getId()).forEach(staff -> clinics.put(staff.getClinic().getId(), staff.getClinic()));
        clinicDoctorRepository.findByDoctor_User_IdAndIsActiveTrue(user.getId())
                .forEach(affiliation -> clinics.put(affiliation.getClinic().getId(), affiliation.getClinic()));
        return clinics.values().stream()
                .sorted(Comparator.comparing(clinic -> clinic.getName() == null ? "" : clinic.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(clinic -> clinicModel(clinic, user))
                .toList();
    }

    @Override
    public List<ClinicModel> listAllClinics() {
        // Admin clinics tab: organization clinics only (personal practices stay on Doctors).
        return clinicDao.findAllOrganizationFetchOwner().stream()
                .sorted(Comparator.comparing(clinic -> clinic.getName() == null ? "" : clinic.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(this::clinicModel)
                .toList();
    }

    @Override
    @Transactional
    public ClinicModel create(ClinicRequest request, String email) {
        User owner = userDao.userByEmail(email);
        if (!hasClinicManagerRole(owner)) {
            throw new CustomException("Only a clinic account can register a clinic", HttpStatus.FORBIDDEN);
        }
        Clinic clinic = Clinic.builder().name(request.name())
                .licenseNumber(request.licenseNumber()).address(request.address()).phone(request.phone()).email(request.email())
                .timezone(request.timezone()).operatingHours(request.operatingHours()).owner(owner).status(ClinicStatus.PENDING)
                .city(blankToNull(request.city())).latitude(request.latitude()).longitude(request.longitude())
                .profileImageUrl(blankToNull(request.profileImageUrl()))
                .build();
        clinic = clinicDao.saveClinic(clinic);
        return clinicModel(clinic);
    }

    @Override
    public ClinicModel get(String clinicUuid, String email) {
        return clinicModel(access(clinicUuid, email), userDao.userByEmail(email));
    }

    @Override
    public ClinicModel getByUuidForAdmin(String clinicUuid) {
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        return clinicModel(clinic);
    }

    @Override
    @Transactional
    public ClinicModel updateStatusForAdmin(String clinicUuid, ClinicStatus status) {
        if (status != ClinicStatus.VERIFIED && status != ClinicStatus.REJECTED) {
            throw new CustomException("Admin can only set VERIFIED or REJECTED", HttpStatus.BAD_REQUEST);
        }
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        if (clinic.getStatus() == ClinicStatus.SHUTDOWN) {
            throw new CustomException("Reopen the clinic before changing verification status", HttpStatus.BAD_REQUEST);
        }
        clinic.setStatus(status);
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    @Transactional
    public ClinicModel update(String clinicUuid, ClinicRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        requireOperational(clinic);
        clinic.setName(request.name());
        clinic.setLicenseNumber(request.licenseNumber());
        clinic.setAddress(request.address());
        clinic.setPhone(request.phone());
        clinic.setEmail(request.email());
        clinic.setTimezone(request.timezone());
        clinic.setOperatingHours(request.operatingHours());
        if (request.city() != null) {
            clinic.setCity(blankToNull(request.city()));
        }
        if (request.latitude() != null) {
            clinic.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            clinic.setLongitude(request.longitude());
        }
        if (request.profileImageUrl() != null) {
            clinic.setProfileImageUrl(blankToNull(request.profileImageUrl()));
        }
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    public void requireClinicManager(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
    }

    @Override
    public ClinicModel switchClinic(String clinicUuid, String email) {
        return clinicModel(access(clinicUuid, email), userDao.userByEmail(email));
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
        List<ClinicDoctor> affiliations = clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId());
        double weightedSum = 0;
        long weight = 0;
        for (ClinicDoctor affiliation : affiliations) {
            DoctorProfile d = affiliation.getDoctor();
            if (d == null || d.getRating() == null || d.getReviewsCount() == null || d.getReviewsCount() <= 0) {
                continue;
            }
            weightedSum += d.getRating() * d.getReviewsCount();
            weight += d.getReviewsCount();
        }
        Double clinicRating = weight == 0 ? null : Math.round((weightedSum / weight) * 10.0) / 10.0;
        String label = ratingLabel(clinicRating);
        return new ClinicStatsModel(diagnosed, patients, clinicRating, weight, label);
    }

    @Override
    public List<DoctorModel> doctors(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        return clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).stream().map(this::doctorModel).toList();
    }

    @Override
    @Transactional
    public ClinicDoctorDetailModel doctorDetail(String clinicUuid, String doctorUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        User viewer = userDao.userByEmail(email);
        ClinicDoctor affiliation = clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(clinic.getId(), doctorUuid)
                .orElseThrow(() -> new ResourceNotFoundException("doctor", "uuid", doctorUuid));
        DoctorProfile doctor = affiliation.getDoctor();
        User user = doctor.getUser();
        // Full dossier (documents, KYC checks, patients) vs clinic directory card (name, specialty, contact).
        boolean includeCredentials = canViewDoctorCertificates(clinic, viewer, doctor);

        List<ClinicDoctorPatientModel> patientList = includeCredentials
                ? treatedPatientsForDoctor(clinic, doctor)
                : List.of();

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
                includeCredentials ? doctor.getLicenseNumber() : null,
                doctor.getBio(),
                doctor.getPhotoUrl(),
                doctor.getExperienceYears(),
                affiliation.getRole(),
                affiliation.getIsActive(),
                affiliation.getJoinedAt() == null ? null : affiliation.getJoinedAt().toString(),
                includeCredentials && doctor.getStatus() != null ? doctor.getStatus().name() : null,
                includeCredentials ? doctor.getDegreeCertificateUrl() : null,
                includeCredentials ? doctor.getRegistrationCertificateUrl() : null,
                includeCredentials ? doctor.getGovernmentIdUrl() : null,
                includeCredentials ? doctor.getLicenseDocumentUrl() : null,
                includeCredentials ? doctor.getClinicPhotosUrls() : null,
                includeCredentials && doctor.isEmailOtpVerified(),
                includeCredentials && doctor.isPhoneOtpVerified(),
                includeCredentials && doctor.isCheckMobileOtp(),
                includeCredentials && doctor.isCheckEmailOtp(),
                includeCredentials && doctor.isCheckGovernmentId(),
                includeCredentials && doctor.isCheckDegree(),
                includeCredentials && doctor.isCheckRegistrationCertificate(),
                includeCredentials && doctor.isCheckClinicAddress(),
                includeCredentials && doctor.isCheckRegistrationNumber(),
                includeCredentials && doctor.isCheckGoogleMapsMatch(),
                includeCredentials && doctor.isCheckClinicPhotos(),
                includeCredentials && doctor.getSubmittedAt() != null ? doctor.getSubmittedAt().toString() : null,
                includeCredentials && doctor.getReviewedAt() != null ? doctor.getReviewedAt().toString() : null,
                includeCredentials ? doctor.getReviewNotes() : null,
                doctor.getRating(),
                doctor.getReviewsCount(),
                ratingLabel(doctor.getRating()),
                patientList);
    }

    /** Pets this doctor treated at this clinic (not waitlist / bookings-only). */
    private List<ClinicDoctorPatientModel> treatedPatientsForDoctor(Clinic clinic, DoctorProfile doctor) {
        Map<String, ClinicDoctorPatientModel> patients = new HashMap<>();
        Set<VisitStatus> treated = EnumSet.of(
                VisitStatus.IN_PROGRESS, VisitStatus.CHECKING_OUT, VisitStatus.COMPLETED);
        for (Visit visit : visitDao.findByClinicAndDoctor(clinic.getId(), doctor.getId())) {
            if (!treated.contains(visit.getStatus())) {
                continue;
            }
            Pet pet = visit.getPet();
            ClinicPetOwner owner = visit.getClinicOwner();
            if (pet == null || owner == null) {
                continue;
            }
            LocalDateTime when = visit.getCompletedAt() != null ? visit.getCompletedAt()
                    : visit.getStartedAt() != null ? visit.getStartedAt()
                            : visit.getCheckedInAt() != null ? visit.getCheckedInAt() : visit.getCreatedAt();
            patients.merge(pet.getUuid(),
                    new ClinicDoctorPatientModel(toPetListModel(pet), ownerSummary(owner), 1, when),
                    (existing, added) -> new ClinicDoctorPatientModel(existing.pet(), existing.owner(),
                            existing.appointmentCount() + 1,
                            laterOf(existing.lastAppointment(), added.lastAppointment())));
        }
        return patients.values().stream()
                .sorted(Comparator
                        .comparing(ClinicDoctorPatientModel::lastAppointment, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(p -> p.pet().name(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    /**
     * Previously copied pets from a doctor's other clinics / personal roster into this clinic.
     * Disabled — clinic data must stay isolated from doctor personal branches.
     */
    private void importDoctorPatientsIntoClinic(Clinic target, DoctorProfile doctor) {
        // no-op
    }

    private ClinicPetOwner findOrCreateImportedOwner(Clinic target, ClinicPetOwner source, DoctorProfile doctor) {
        String email = source.getEmail() == null ? null : source.getEmail().trim().toLowerCase();
        if (email != null) {
            Optional<ClinicPetOwner> existing = clinicPetOwnerRepository
                    .findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(target.getId(), email);
            if (existing.isPresent()) {
                ClinicPetOwner owner = existing.get();
                String tag = "doctor:" + doctor.getUuid();
                if (owner.getNotes() == null || !owner.getNotes().contains(tag)) {
                    owner.setNotes(((owner.getNotes() == null ? "" : owner.getNotes() + "\n") + tag).trim());
                    owner = clinicPetOwnerRepository.save(owner);
                }
                return owner;
            }
        }
        String tag = "Imported with doctor " + fullName(doctor.getUser()) + "\ndoctor:" + doctor.getUuid();
        return clinicPetOwnerRepository.save(ClinicPetOwner.builder()
                .clinic(target)
                .firstName(source.getFirstName())
                .lastName(source.getLastName())
                .email(email != null ? email : "unknown+" + UUID.randomUUID() + "@kittyp.local")
                .phone(source.getPhone() == null || source.getPhone().isBlank() ? "0000000000" : source.getPhone())
                .alternatePhone(source.getAlternatePhone())
                .address(source.getAddress())
                .notes(tag)
                .linkedUser(source.getLinkedUser())
                .build());
    }

    private ClinicPetOwner findOrCreateDoctorAsOwner(Clinic target, User doctorUser, DoctorProfile doctor) {
        String email = doctorUser.getEmail().trim().toLowerCase();
        Optional<ClinicPetOwner> existing = clinicPetOwnerRepository
                .findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(target.getId(), email);
        if (existing.isPresent()) {
            ClinicPetOwner owner = existing.get();
            if (owner.getLinkedUser() == null) {
                owner.setLinkedUser(doctorUser);
                owner = clinicPetOwnerRepository.save(owner);
            }
            return owner;
        }
        String phone = doctorUser.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            phone = doctor.getPhoneNumber();
        }
        if (phone == null || phone.isBlank()) {
            phone = "0000000000";
        }
        return clinicPetOwnerRepository.save(ClinicPetOwner.builder()
                .clinic(target)
                .firstName(doctorUser.getFirstName() == null ? "Doctor" : doctorUser.getFirstName())
                .lastName(doctorUser.getLastName())
                .email(email)
                .phone(phone)
                .notes("Doctor as pet owner\ndoctor:" + doctor.getUuid())
                .linkedUser(doctorUser)
                .build());
    }

    private Pet copyPetToClinic(Clinic target, ClinicPetOwner owner, Pet source, String importKey) {
        return Pet.builder()
                .clinic(target)
                .clinicOwner(owner)
                .name(source.getName())
                .type(source.getType())
                .breed(source.getBreed())
                .gender(source.getGender())
                .dateOfBirth(source.getDateOfBirth())
                .weight(source.getWeight())
                .microchipNumber(source.getMicrochipNumber())
                .profilePicture(source.getProfilePicture())
                .patientNumber(importKey)
                .activityLevel(source.getActivityLevel())
                .currentFoodBrand(source.getCurrentFoodBrand())
                .healthConditions(source.getHealthConditions())
                .allergies(source.getAllergies())
                .isNeutered(source.isNeutered())
                .registeredAt(source.getRegisteredAt() != null ? source.getRegisteredAt() : LocalDate.now())
                .build();
    }

    @Override
    @Transactional
    public DoctorInviteModel inviteDoctor(String clinicUuid, DoctorInviteRequest request, String email) {
        User inviter = userDao.userByEmail(email);
        requireCanInviteDoctors(inviter);
        Clinic clinic = access(clinicUuid, email);
        requireActivated(clinic);
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
            requirePracticeReady(profile);
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
                DoctorProfile existingProfile = doctorProfileDao.findByUserId(existingUser.getId());
                if (existingProfile != null) {
                    requirePracticeReady(existingProfile);
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
        requireCanInviteDoctors(caller);
        return resolveDoctorLookup(doctorUuid);
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
        requireCanInviteDoctors(userDao.userByEmail(email));
        Clinic clinic = access(clinicUuid, email);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentCutoff = now.minusDays(14);
        return clinicDoctorInviteRepository.findByClinic_IdOrderByCreatedAtDesc(clinic.getId()).stream()
                .map(invite -> {
                    if (invite.getStatus() == ClinicDoctorInviteStatus.PENDING
                            && invite.getExpiresAt().isBefore(now)) {
                        invite.setStatus(ClinicDoctorInviteStatus.EXPIRED);
                        clinicDoctorInviteRepository.save(invite);
                    }
                    return invite;
                })
                .filter(invite -> {
                    if (invite.getStatus() == ClinicDoctorInviteStatus.PENDING) {
                        return true;
                    }
                    // Surface accept/reject/revoke responses in the clinic notification inbox for 14 days.
                    LocalDateTime stamp = invite.getUpdatedAt() != null ? invite.getUpdatedAt() : invite.getCreatedAt();
                    return stamp != null && !stamp.isBefore(recentCutoff)
                            && (invite.getStatus() == ClinicDoctorInviteStatus.ACCEPTED
                                    || invite.getStatus() == ClinicDoctorInviteStatus.REJECTED
                                    || invite.getStatus() == ClinicDoctorInviteStatus.REVOKED
                                    || invite.getStatus() == ClinicDoctorInviteStatus.EXPIRED);
                })
                .map(this::inviteModel)
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
        User caller = userDao.userByEmail(email);
        requireCanInviteDoctors(caller);
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, caller);
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
    @Transactional
    public DoctorInviteModel remindDoctorInvite(String clinicUuid, String inviteUuid, String email) {
        User caller = userDao.userByEmail(email);
        requireCanInviteDoctors(caller);
        Clinic clinic = access(clinicUuid, email);
        requireActivated(clinic);
        requireClinicManager(clinic, caller);
        ClinicDoctorInvite invite = clinicDoctorInviteRepository.findByUuid(inviteUuid)
                .orElseThrow(() -> new ResourceNotFoundException("invite", "uuid", inviteUuid));
        if (!invite.getClinic().getId().equals(clinic.getId())) {
            throw new AccessDeniedException("Invite does not belong to this clinic");
        }
        if (invite.getStatus() != ClinicDoctorInviteStatus.PENDING) {
            throw new CustomException("Only pending invites can be reminded", HttpStatus.BAD_REQUEST);
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(ClinicDoctorInviteStatus.EXPIRED);
            clinicDoctorInviteRepository.save(invite);
            throw new CustomException("This invite has expired — send a new invite", HttpStatus.BAD_REQUEST);
        }
        if (!canRemindInvite(invite)) {
            throw new CustomException(
                    "Reminder available 24 hours after the invite (or last reminder). Try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String acceptUrl = frontendBaseUrl.replaceAll("/$", "") + "/clinic-invite/accept?token=" + invite.getToken();
        zeptoMailService.sendClinicDoctorInviteReminderEmail(invite.getEmail(), invite.getDoctorName(),
                clinic.getName(), acceptUrl);
        invite.setLastRemindedAt(LocalDateTime.now());
        invite = clinicDoctorInviteRepository.save(invite);
        return inviteModel(invite, true);
    }

    @Override
    public DoctorInvitePreview previewInvite(String token) {
        ClinicDoctorInvite invite = requireInviteByToken(token);
        boolean expired = invite.getExpiresAt().isBefore(LocalDateTime.now())
                || invite.getStatus() == ClinicDoctorInviteStatus.EXPIRED;
        boolean accepted = invite.getStatus() == ClinicDoctorInviteStatus.ACCEPTED;
        return new DoctorInvitePreview(invite.getClinic().getName(), invite.getDoctorName(),
                maskEmail(invite.getEmail()), expired, accepted, invite.getStatus().name(),
                invite.getClinic().getUuid());
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
        requireActivated(clinic);

        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            profile = doctorProfileDao.save(DoctorProfile.builder()
                    .user(user)
                    .phoneNumber(user.getPhoneNumber())
                    .status(DoctorStatus.REGISTERED)
                    .build());
        }
        requirePracticeReady(profile);

        if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), user.getId())) {
            invite.setStatus(ClinicDoctorInviteStatus.ACCEPTED);
            clinicDoctorInviteRepository.save(invite);
            notifyClinicOfInviteResponse(invite, true);
            ClinicDoctor existing = clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).stream()
                    .filter(a -> a.getDoctor().getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow();
            return doctorModel(existing);
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
        notifyClinicOfInviteResponse(invite, true);

        return doctorModel(affiliation);
    }

    @Override
    @Transactional
    public void rejectInvite(String token, String email) {
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
            throw new CustomException("Sign in with the invited email (" + invite.getEmail() + ") to respond",
                    HttpStatus.FORBIDDEN);
        }

        invite.setStatus(ClinicDoctorInviteStatus.REJECTED);
        clinicDoctorInviteRepository.save(invite);
        notifyClinicOfInviteResponse(invite, false);
    }

    @Override
    @Transactional
    public StaffInviteModel inviteStaff(String clinicUuid, StaffInviteRequest request, String email) {
        User inviter = userDao.userByEmail(email);
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, inviter);
        requireActivated(clinic);

        long invitesLastHour = clinicStaffInviteRepository.countByClinic_IdAndCreatedAtAfter(
                clinic.getId(), LocalDateTime.now().minusHours(1));
        if (invitesLastHour >= 20) {
            throw new CustomException("Too many staff invites sent recently. Try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new CustomException("Staff name is required", HttpStatus.BAD_REQUEST);
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new CustomException("Staff email is required", HttpStatus.BAD_REQUEST);
        }
        String inviteEmail = request.email().trim().toLowerCase();
        if (!inviteEmail.contains("@") || inviteEmail.length() < 5) {
            throw new CustomException("Enter a valid staff email", HttpStatus.BAD_REQUEST);
        }
        if (inviteEmail.equalsIgnoreCase(inviter.getEmail())) {
            throw new CustomException("You cannot invite your own account as staff", HttpStatus.BAD_REQUEST);
        }

        userRepository.findByEmailIgnoreCase(inviteEmail).ifPresent(existing -> {
            if (!isStaffOnly(existing)) {
                throw new CustomException(
                        "This email already has a KittyP account. Invite a dedicated staff work email.",
                        HttpStatus.CONFLICT);
            }
            if (clinicStaffDao.existsActiveByUserId(existing.getId())) {
                throw new CustomException(
                        "This person is already active staff at a clinic. Disable them there first.",
                        HttpStatus.CONFLICT);
            }
        });

        ClinicStaffInvite invite = clinicStaffInviteRepository
                .findByClinic_IdAndEmailIgnoreCaseAndStatus(clinic.getId(), inviteEmail, ClinicStaffInviteStatus.PENDING)
                .orElse(null);

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        String staffName = request.name().trim();

        if (invite == null) {
            invite = ClinicStaffInvite.builder()
                    .uuid(UUID.randomUUID().toString())
                    .clinic(clinic)
                    .email(inviteEmail)
                    .staffName(staffName)
                    .token(token)
                    .status(ClinicStaffInviteStatus.PENDING)
                    .invitedByUserId(inviter.getId())
                    .expiresAt(expiresAt)
                    .build();
        } else {
            invite.setStaffName(staffName);
            invite.setToken(token);
            invite.setExpiresAt(expiresAt);
            invite.setInvitedByUserId(inviter.getId());
        }
        invite = clinicStaffInviteRepository.save(invite);

        String acceptUrl = frontendBaseUrl.replaceAll("/$", "") + "/staff-invite/accept?token=" + invite.getToken();
        log.info("Sending staff invite email to {} for clinic {}", inviteEmail, clinic.getName());
        log.info("Accept URL: {}", acceptUrl);
        zeptoMailService.sendClinicStaffInviteEmail(inviteEmail, staffName, clinic.getName(), acceptUrl);
        return staffInviteModel(invite, true);
    }

    @Override
    @Transactional
    public List<StaffInviteModel> listStaffInvites(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        LocalDateTime now = LocalDateTime.now();
        return clinicStaffInviteRepository.findByClinic_IdOrderByCreatedAtDesc(clinic.getId()).stream()
                .map(invite -> {
                    if (invite.getStatus() == ClinicStaffInviteStatus.PENDING
                            && invite.getExpiresAt().isBefore(now)) {
                        invite.setStatus(ClinicStaffInviteStatus.EXPIRED);
                        clinicStaffInviteRepository.save(invite);
                    }
                    return invite;
                })
                .filter(invite -> invite.getStatus() == ClinicStaffInviteStatus.PENDING
                        || invite.getStatus() == ClinicStaffInviteStatus.ACCEPTED)
                .map(invite -> staffInviteModel(invite, true))
                .toList();
    }

    @Override
    public List<StaffMemberModel> listStaff(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        return clinicStaffDao.findActiveByClinicId(clinic.getId()).stream()
                .map(this::staffMemberModel)
                .toList();
    }

    @Override
    @Transactional
    public void revokeStaffInvite(String clinicUuid, String inviteUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, userDao.userByEmail(email));
        ClinicStaffInvite invite = clinicStaffInviteRepository.findByUuid(inviteUuid)
                .orElseThrow(() -> new ResourceNotFoundException("staff invite", "uuid", inviteUuid));
        if (!invite.getClinic().getId().equals(clinic.getId())) {
            throw new CustomException("Invite does not belong to this clinic", HttpStatus.FORBIDDEN);
        }
        if (invite.getStatus() != ClinicStaffInviteStatus.PENDING) {
            throw new CustomException("Only pending invites can be revoked", HttpStatus.BAD_REQUEST);
        }
        invite.setStatus(ClinicStaffInviteStatus.REVOKED);
        clinicStaffInviteRepository.save(invite);
    }

    @Override
    @Transactional
    public void disableStaff(String clinicUuid, String userUuid, String email) {
        User actor = userDao.userByEmail(email);
        Clinic clinic = access(clinicUuid, email);
        requireClinicManager(clinic, actor);
        User staffUser = userDao.userByUuid(userUuid);
        if (staffUser.getId().equals(actor.getId())) {
            throw new CustomException("You cannot disable your own staff account", HttpStatus.BAD_REQUEST);
        }
        ClinicStaff membership = clinicStaffDao.findLatestByClinicAndUser(clinic.getId(), staffUser.getId())
                .filter(row -> Boolean.TRUE.equals(row.getIsActive()))
                .orElseThrow(() -> new CustomException("This person is not active staff at this clinic",
                        HttpStatus.NOT_FOUND));
        if (!isStaffOnly(staffUser)) {
            throw new CustomException("Only dedicated staff accounts can be disabled from here", HttpStatus.BAD_REQUEST);
        }
        membership.setIsActive(false);
        clinicStaffDao.save(membership);
        staffUser.setEnabled(false);
        staffUser.setIsActive(false);
        userDao.saveUser(staffUser);
    }

    @Override
    public StaffInvitePreview previewStaffInvite(String token) {
        ClinicStaffInvite invite = requireStaffInviteByToken(token);
        boolean expired = invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now());
        boolean accepted = invite.getStatus() == ClinicStaffInviteStatus.ACCEPTED;
        ClinicStaffInviteStatus status = invite.getStatus();
        if (expired && status == ClinicStaffInviteStatus.PENDING) {
            status = ClinicStaffInviteStatus.EXPIRED;
        }
        return new StaffInvitePreview(invite.getClinic().getName(), invite.getStaffName(), invite.getEmail(), expired,
                accepted, status.name());
    }

    @Override
    @Transactional
    public StaffMemberModel completeStaffInvite(String token, StaffInviteCompleteRequest request) {
        ClinicStaffInvite invite = requireStaffInviteByToken(token);
        if (invite.getStatus() != ClinicStaffInviteStatus.PENDING) {
            throw new CustomException("This invite is no longer valid", HttpStatus.BAD_REQUEST);
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(ClinicStaffInviteStatus.EXPIRED);
            clinicStaffInviteRepository.save(invite);
            throw new CustomException("This invite has expired", HttpStatus.BAD_REQUEST);
        }
        Clinic clinic = invite.getClinic();
        requireActivated(clinic);

        User user = userRepository.findByEmailIgnoreCase(invite.getEmail()).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(invite.getEmail())
                    .password(passwordEncoder.encode(request.password()))
                    .firstName(request.firstName().trim())
                    .lastName(request.lastName() == null ? null : request.lastName().trim())
                    .enabled(true)
                    .build();
            user.setIsActive(true);
            user.addRole(roleDao.roleByName(ERole.ROLE_CLINIC_STAFF));
            user = userDao.saveUser(user);
        } else {
            if (!isStaffOnly(user)) {
                throw new CustomException(
                        "This email already has a KittyP account. Invite a dedicated staff work email.",
                        HttpStatus.CONFLICT);
            }
            if (clinicStaffDao.existsActiveByUserId(user.getId())
                    && !clinicStaffDao.isActiveMember(clinic.getId(), user.getId())) {
                throw new CustomException(
                        "This person is already active staff at a clinic. Disable them there first.",
                        HttpStatus.CONFLICT);
            }
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setFirstName(request.firstName().trim());
            user.setLastName(request.lastName() == null ? null : request.lastName().trim());
            user.setEnabled(true);
            user.setIsActive(true);
            user = userDao.saveUser(user);
        }

        ClinicStaff membership = clinicStaffDao.findLatestByClinicAndUser(clinic.getId(), user.getId()).orElse(null);
        if (membership == null) {
            membership = ClinicStaff.builder()
                    .clinic(clinic)
                    .user(user)
                    .role("staff")
                    .joinedAt(LocalDateTime.now())
                    .build();
            membership.setIsActive(true);
        } else {
            membership.setIsActive(true);
            membership.setRole("staff");
            membership.setJoinedAt(LocalDateTime.now());
        }
        membership = clinicStaffDao.save(membership);

        invite.setStatus(ClinicStaffInviteStatus.ACCEPTED);
        clinicStaffInviteRepository.save(invite);
        return staffMemberModel(membership);
    }

    private ClinicStaffInvite requireStaffInviteByToken(String token) {
        return clinicStaffInviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("staff invite", "token", token));
    }

    private StaffInviteModel staffInviteModel(ClinicStaffInvite invite, boolean includeToken) {
        String createdAt = invite.getCreatedAt() != null ? invite.getCreatedAt().toString() : null;
        return new StaffInviteModel(invite.getUuid(), invite.getEmail(), invite.getStaffName(),
                invite.getStatus().name(), invite.getExpiresAt().toString(), invite.getClinic().getUuid(),
                invite.getClinic().getName(), includeToken ? invite.getToken() : null, createdAt);
    }

    private StaffMemberModel staffMemberModel(ClinicStaff membership) {
        User user = membership.getUser();
        boolean enabled = user.isEnabled() && (user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive()));
        return new StaffMemberModel(user.getUuid(), user.getEmail(), fullName(user), membership.getRole(),
                membership.getJoinedAt() == null ? null : membership.getJoinedAt().toString(), enabled);
    }

    private static boolean isStaffOnly(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return false;
        }
        return user.getUserRoles().stream().allMatch(userRole -> userRole.getRole() != null
                && userRole.getRole().getName() == ERole.ROLE_CLINIC_STAFF);
    }

    private void notifyClinicOfInviteResponse(ClinicDoctorInvite invite, boolean accepted) {
        Clinic clinic = invite.getClinic();
        String action = accepted ? "accepted" : "declined";
        String payload = String.format("%s (%s) %s the invite to join %s.", invite.getDoctorName(), invite.getEmail(),
                action, clinic.getName());

        User notifyUser = clinic.getOwner();
        if (notifyUser != null) {
            Runnable persist = () -> {
                try {
                    notificationLogRepository.save(NotificationLog.builder()
                            .user(notifyUser)
                            .type(NotificationType.CLINIC_DOCTOR_INVITE_RESPONSE)
                            .payload(payload)
                            .sentAt(LocalDateTime.now())
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to log clinic invite response notification: {}", e.getMessage());
                }
            };
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        persist.run();
                    }
                });
            } else {
                persist.run();
            }
        }

        String clinicEmail = clinic.getEmail();
        if ((clinicEmail == null || clinicEmail.isBlank()) && clinic.getOwner() != null) {
            clinicEmail = clinic.getOwner().getEmail();
        }
        zeptoMailService.sendClinicDoctorInviteResponseEmail(clinicEmail, clinic.getName(), invite.getDoctorName(),
                invite.getEmail(), accepted);
    }

    private static boolean canRemindInvite(ClinicDoctorInvite invite) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = invite.getCreatedAt() != null ? invite.getCreatedAt() : now;
        if (ChronoUnit.HOURS.between(created, now) < 24) {
            return false;
        }
        if (invite.getLastRemindedAt() == null) {
            return true;
        }
        return ChronoUnit.HOURS.between(invite.getLastRemindedAt(), now) >= 24;
    }

    private ClinicDoctorInvite requireInviteByToken(String token) {
        return clinicDoctorInviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("invite", "token", token));
    }

    private DoctorInviteModel inviteModel(ClinicDoctorInvite invite) {
        return inviteModel(invite, false);
    }

    private DoctorInviteModel inviteModel(ClinicDoctorInvite invite, boolean includeToken) {
        String createdAt = invite.getCreatedAt() != null ? invite.getCreatedAt().toString() : null;
        String lastRemindedAt = invite.getLastRemindedAt() != null ? invite.getLastRemindedAt().toString() : null;
        Boolean canRemind = invite.getStatus() == ClinicDoctorInviteStatus.PENDING && canRemindInvite(invite);
        return new DoctorInviteModel(invite.getUuid(), invite.getEmail(), invite.getDoctorName(),
                invite.getStatus().name(), invite.getExpiresAt().toString(), invite.getClinic().getUuid(),
                invite.getClinic().getName(), includeToken ? invite.getToken() : null, createdAt, lastRemindedAt,
                canRemind);
    }

    @Override
    @Transactional
    public List<PatientModel> patients(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        hideLegacyDoctorImportPets(clinic);
        User viewer = userDao.userByEmail(email);
        if (rosterVisitScoped(clinic, viewer)) {
            Map<String, PatientModel> visited = new HashMap<>();
            for (String petUuid : visitedPetUuids(clinic)) {
                Pet pet = petsRepository.findByUuid(petUuid);
                if (pet == null || !Boolean.TRUE.equals(pet.getIsActive()) || isLegacyDoctorImport(pet)) {
                    continue;
                }
                visited.put(petUuid, patientModelForPet(pet, null));
            }
            return visited.values().stream()
                    .sorted(Comparator.comparing(PatientModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        Map<String, PatientModel> activity = patientMap(clinic);
        Map<String, PatientModel> merged = new HashMap<>(activity);
        for (Pet pet : petsRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            if (isLegacyDoctorImport(pet)) {
                continue;
            }
            PatientModel clinicPet = toClinicPatientModel(pet);
            PatientModel prior = activity.get(pet.getUuid());
            LocalDateTime last = laterOf(clinicPet.lastVisit(), prior == null ? null : prior.lastVisit());
            merged.put(pet.getUuid(), withLastVisit(clinicPet, last));
        }
        for (ClinicPetEnrollment enrollment : clinicPetEnrollmentRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            Pet pet = enrollment.getPet();
            if (pet == null || !Boolean.TRUE.equals(pet.getIsActive()) || isLegacyDoctorImport(pet)) {
                continue;
            }
            PatientModel clinicPet = toClinicPatientModel(pet, enrollment.getClinicOwner());
            PatientModel prior = merged.get(pet.getUuid());
            LocalDateTime last = laterOf(clinicPet.lastVisit(), prior == null ? null : prior.lastVisit());
            merged.put(pet.getUuid(), withLastVisit(clinicPet, last));
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(PatientModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public PatientDetailModel patientDetail(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        AccessiblePet access = resolveAccessiblePet(clinic, petUuid, email);

        if (access.clinicOwner() != null) {
            return clinicRegisteredPatientDetail(clinic, access.pet(), access.clinicOwner());
        }
        if (access.platformOwner() != null) {
            return platformOwnerPatientDetail(clinic, access.pet(), access.platformOwner());
        }

        Map<String, PatientModel> clinicPatients = patientMap(clinic);
        PatientModel patient = clinicPatients.get(petUuid);
        if (patient == null) {
            throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
        }
        User owner = userDao.userByPetUuid(petUuid);
        OwnerSummaryModel ownerSummary = new OwnerSummaryModel(owner.getUuid(), fullName(owner), owner.getEmail(),
                formatPhone(owner), primaryAddress(owner));
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
                blankToNull(request.petWeight()), blankToNull(request.petMicrochipNumber()),
                blankToNull(request.petPhotoUrl()), null);
        if (owner.getLinkedUser() != null) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
        }

        return clinicRegisteredPatientDetail(clinic, pet);
    }

    @Override
    @Transactional
    public List<ClinicOwnerModel> listOwners(String clinicUuid, String q, String email, boolean emailOrIdOnly) {
        Clinic clinic = access(clinicUuid, email);
        hideLegacyDoctorImportPets(clinic);
        User viewer = userDao.userByEmail(email);
        List<ClinicPetOwner> owners;
        if (q == null || q.isBlank()) {
            owners = emailOrIdOnly
                    ? List.of()
                    : clinicPetOwnerRepository.findByClinic_IdAndIsActiveTrue(clinic.getId());
        } else if (emailOrIdOnly) {
            owners = clinicPetOwnerRepository.searchByClinicEmailOrId(clinic.getId(), q.trim());
        } else {
            owners = clinicPetOwnerRepository.searchByClinic(clinic.getId(), q.trim());
        }
        Set<String> visitedPets = rosterVisitScoped(clinic, viewer) && !emailOrIdOnly
                ? visitedPetUuids(clinic)
                : null;
        return owners.stream()
                .map(this::toOwnerModel)
                .filter(o -> o.petCount() > 0)
                .filter(o -> visitedPets == null
                        || (o.pets() != null && o.pets().stream().anyMatch(p -> visitedPets.contains(p.petUuid()))))
                .sorted(Comparator.comparing(ClinicOwnerModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional
    public PaginationModel<ClinicOwnerModel> pageOwners(String clinicUuid, String q, String email,
            boolean emailOrIdOnly, Integer pageNumber, Integer pageSize) {
        return PaginationSupport.slice(listOwners(clinicUuid, q, email, emailOrIdOnly), pageNumber, pageSize);
    }

    @Override
    public List<PlatformUserSearchModel> searchPlatformUsers(String clinicUuid, String q, String email,
            boolean emailOrIdOnly) {
        Clinic clinic = access(clinicUuid, email);
        if (q == null || q.isBlank()) {
            return List.of();
        }
        String raw = q.trim();
        // Block broad enumeration (single-letter harvest of the entire user table).
        if (raw.length() < 3) {
            return List.of();
        }
        // Strip LIKE wildcards — never let callers broaden the pattern.
        String query = raw.replace("%", "").replace("_", "").trim();
        if (query.length() < 3) {
            return List.of();
        }
        // Fresh DB read — ROLE_USER only; includes accounts created moments ago.
        LinkedHashMap<String, User> byUuid = new LinkedHashMap<>();
        List<User> users = emailOrIdOnly
                ? userRepository.searchActiveUsersByEmailOrUuid(query, PageRequest.of(0, 20))
                : userRepository.searchActiveUsers(query, PageRequest.of(0, 20));
        for (User u : users) {
            byUuid.put(u.getUuid(), u);
        }
        if (emailOrIdOnly) {
            userRepository.findByUuidIgnoreCase(query).ifPresent(u -> byUuid.putIfAbsent(u.getUuid(), u));
            petsRepository.findByUuidIgnoreCase(query).ifPresent(pet -> userDao
                    .findOptionalByPetUuid(pet.getUuid())
                    .ifPresent(u -> byUuid.putIfAbsent(u.getUuid(), u)));
        }
        return byUuid.values().stream().map(u -> {
            ClinicPetOwner existing = clinicPetOwnerRepository
                    .findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(clinic.getId(), u.getId())
                    .or(() -> {
                        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(u.getEmail());
                        if (ownerEmail == null) {
                            return Optional.empty();
                        }
                        return clinicPetOwnerRepository.findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(
                                clinic.getId(), ownerEmail);
                    })
                    .orElse(null);
            String name = Stream.of(u.getFirstName(), u.getLastName())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(" "));
            if (name.isBlank()) {
                name = u.getEmail();
            }
            return new PlatformUserSearchModel(
                    u.getUuid(),
                    name,
                    u.getEmail(),
                    formatClinicPhone(u.getPhoneNumber()),
                    existing == null ? null : existing.getUuid(),
                    existing != null);
        }).toList();
    }

    @Override
    @Transactional
    public ClinicOwnerModel ensureOwnerFromUser(String clinicUuid, String userUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        if (userUuid == null || userUuid.isBlank() || userUuid.length() > 64) {
            throw new CustomException("Invalid user id", HttpStatus.BAD_REQUEST);
        }
        User platformUser = userRepository.findByUuid(userUuid.trim())
                .orElseThrow(() -> new ResourceNotFoundException("user", "uuid", userUuid));
        if (Boolean.FALSE.equals(platformUser.getIsActive()) || !platformUser.isEnabled()) {
            throw new CustomException("User account is not active", HttpStatus.BAD_REQUEST);
        }
        boolean isPetParent = platformUser.getUserRoles() != null && platformUser.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole() != null && ERole.ROLE_USER.equals(ur.getRole().getName()));
        if (!isPetParent) {
            throw new CustomException("Only pet-parent KittyP accounts can be added as clients",
                    HttpStatus.BAD_REQUEST);
        }

        ClinicPetOwner owner = clinicPetOwnerRepository
                .findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(clinic.getId(), platformUser.getId())
                .orElse(null);
        if (owner == null) {
            String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(platformUser.getEmail());
            if (ownerEmail != null) {
                owner = clinicPetOwnerRepository
                        .findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(clinic.getId(), ownerEmail)
                        .orElse(null);
            }
        }
        if (owner == null) {
            String phone = ClinicOwnerUserLinkService.normalizePhoneDigits(platformUser.getPhoneNumber());
            if (phone == null || !phone.matches("\\d{10}")) {
                throw new CustomException(
                        "This KittyP account has no valid phone on file. Ask them to update their profile, or add the client manually with a phone number.",
                        HttpStatus.BAD_REQUEST);
            }
            owner = ClinicPetOwner.builder()
                    .clinic(clinic)
                    .firstName(platformUser.getFirstName() == null || platformUser.getFirstName().isBlank()
                            ? "Client"
                            : platformUser.getFirstName().trim())
                    .lastName(platformUser.getLastName() == null ? "" : platformUser.getLastName().trim())
                    .email(ClinicOwnerUserLinkService.normalizeEmail(platformUser.getEmail()))
                    .phone(phone)
                    .linkedUser(platformUser)
                    .build();
            owner = clinicPetOwnerRepository.save(owner);
            log.info("Created clinic owner {} linked to user {} (pets not force-attached)", owner.getUuid(),
                    platformUser.getUuid());
        } else if (owner.getLinkedUser() == null) {
            owner.setLinkedUser(platformUser);
            owner = clinicPetOwnerRepository.save(owner);
        }
        // Soft-link only — do not force-attach pets here (parent may have hidden them).
        return toOwnerModel(owner);
    }

    @Override
    @Transactional
    public ClinicPetListModel admitPlatformPet(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        if (petUuid == null || petUuid.isBlank()) {
            throw new CustomException("Pet id is required", HttpStatus.BAD_REQUEST);
        }
        String key = petUuid.trim();
        Pet pet = petsRepository.findByUuidIgnoreCase(key)
                .orElseThrow(() -> new ResourceNotFoundException("pet", "uuid", key));
        if (!Boolean.TRUE.equals(pet.getIsActive()) || Boolean.TRUE.equals(pet.getHiddenFromParent())) {
            throw new CustomException("Pet is not available to admit", HttpStatus.BAD_REQUEST);
        }
        parentBookingEnrollmentService.admitPetToClinic(clinic, pet);
        pet = petsRepository.findByUuidIgnoreCase(key).orElse(pet);
        return toAppointmentPetModel(clinic, pet);
    }

    @Override
    public OwnerEmailLookupModel lookupOwnerByEmail(String clinicUuid, String ownerEmail, String email) {
        Clinic clinic = access(clinicUuid, email);
        String normalized = ClinicOwnerUserLinkService.normalizeEmail(ownerEmail);
        if (normalized == null || normalized.isBlank()) {
            return new OwnerEmailLookupModel(false, null, null, null);
        }
        ClinicPetOwner clinicOwner = clinicPetOwnerRepository
                .findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(clinic.getId(), normalized)
                .orElse(null);
        User platformUser = userRepository.findByEmailIgnoreCase(normalized).orElse(null);
        if (platformUser != null && (Boolean.FALSE.equals(platformUser.getIsActive()) || !platformUser.isEnabled())) {
            platformUser = null;
        }
        if (platformUser != null) {
            boolean isPetParent = platformUser.getUserRoles() != null && platformUser.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole() != null && ERole.ROLE_USER.equals(ur.getRole().getName()));
            if (!isPetParent) {
                platformUser = null;
            }
        }
        if (clinicOwner == null && platformUser == null) {
            return new OwnerEmailLookupModel(false, null, null, null);
        }
        String source;
        if (clinicOwner != null && platformUser != null) {
            source = "BOTH";
        } else if (clinicOwner != null) {
            source = "CLINIC";
        } else {
            source = "PLATFORM";
        }
        ClinicOwnerModel ownerModel = clinicOwner == null ? null : toOwnerModel(clinicOwner);
        PlatformUserSearchModel platformModel = null;
        if (platformUser != null) {
            String name = Stream.of(platformUser.getFirstName(), platformUser.getLastName())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(" "));
            if (name.isBlank()) {
                name = platformUser.getEmail();
            }
            platformModel = new PlatformUserSearchModel(
                    platformUser.getUuid(),
                    name,
                    platformUser.getEmail(),
                    formatClinicPhone(platformUser.getPhoneNumber()),
                    clinicOwner == null ? null : clinicOwner.getUuid(),
                    clinicOwner != null);
        }
        return new OwnerEmailLookupModel(true, source, ownerModel, platformModel);
    }

    @Override
    @Transactional
    public void sendPetConsentOtp(String clinicUuid, String ownerUuid, String petName, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        ClinicPetOwner owner = clinicPetOwnerRepository.findByUuidAndClinic_IdAndIsActiveTrue(ownerUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("owner", "uuid", ownerUuid));
        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(owner.getEmail());
        if (ownerEmail == null || ownerEmail.isBlank()) {
            throw new CustomException("Owner has no email on file for consent OTP", HttpStatus.BAD_REQUEST);
        }
        String name = petName == null ? "" : petName.trim();
        if (name.isBlank()) {
            throw new CustomException("Pet name is required for consent", HttpStatus.BAD_REQUEST);
        }
        String otpKey = VerificationCodeService.clinicPetConsentOtpKey(clinic.getUuid(), ownerEmail, name);
        String code = verificationCodeService.generateCode(otpKey);
        log.info("Clinic pet-consent OTP generated clinic={} ownerEmail={} pet={}", clinic.getUuid(), ownerEmail, name);
        zeptoMailService.sendClinicPetConsentOtpEmail(ownerEmail, clinicOwnerName(owner), clinic.getName(), name, code);
    }

    @Override
    @Transactional
    public void verifyPetConsentOtp(String clinicUuid, String ownerUuid, String petName, String code, String email) {
        Clinic clinic = access(clinicUuid, email);
        ClinicPetOwner owner = clinicPetOwnerRepository.findByUuidAndClinic_IdAndIsActiveTrue(ownerUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("owner", "uuid", ownerUuid));
        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(owner.getEmail());
        if (ownerEmail == null || ownerEmail.isBlank()) {
            throw new CustomException("Owner has no email on file for consent OTP", HttpStatus.BAD_REQUEST);
        }
        String name = petName == null ? "" : petName.trim();
        if (name.isBlank()) {
            throw new CustomException("Pet name is required for consent", HttpStatus.BAD_REQUEST);
        }
        String otpKey = VerificationCodeService.clinicPetConsentOtpKey(clinic.getUuid(), ownerEmail, name);
        boolean ok = verificationCodeService.verifyCode(otpKey, code, true);
        if (!ok) {
            throw new CustomException("Invalid or expired consent code", HttpStatus.BAD_REQUEST);
        }
        verificationCodeService.markVerified(
                VerificationCodeService.clinicPetConsentVerifiedKey(clinic.getUuid(), ownerEmail, name));
    }

    /**
     * Staff adding a pet to an existing owner (schedule / CRM) requires one-pet email consent.
     * Walk-in does not use this path for OTP.
     */
    void requirePetConsentVerified(Clinic clinic, ClinicPetOwner owner, String petName) {
        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(owner.getEmail());
        String name = petName == null ? "" : petName.trim();
        String verifiedKey = VerificationCodeService.clinicPetConsentVerifiedKey(clinic.getUuid(), ownerEmail, name);
        if (!verificationCodeService.isVerified(verifiedKey)) {
            throw new CustomException(
                    "Owner consent required: send and verify the email OTP before adding this pet",
                    HttpStatus.BAD_REQUEST);
        }
        verificationCodeService.clearVerified(verifiedKey);
    }

    @Override
    public void requirePetConsentForEmail(String clinicUuid, String ownerEmail, String petName) {
        String verifiedKey = VerificationCodeService.clinicPetConsentVerifiedKey(clinicUuid, ownerEmail, petName);
        if (!verificationCodeService.isVerified(verifiedKey)) {
            throw new CustomException(
                    "Owner consent required: send and verify the email OTP before adding this pet",
                    HttpStatus.BAD_REQUEST);
        }
        verificationCodeService.clearVerified(verifiedKey);
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

        requirePetConsentVerified(clinic, owner, request.name());

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
    public List<ClinicPetListModel> listPets(String clinicUuid, String q, String email, boolean emailOrIdOnly) {
        Clinic clinic = access(clinicUuid, email);
        hideLegacyDoctorImportPets(clinic);
        User viewer = userDao.userByEmail(email);
        Set<String> visitedPets = rosterVisitScoped(clinic, viewer) && !emailOrIdOnly
                ? visitedPetUuids(clinic)
                : null;
        List<Pet> pets;
        if (q == null || q.isBlank()) {
            pets = emailOrIdOnly ? List.of() : petsRepository.findByClinic_IdAndIsActiveTrue(clinic.getId());
        } else if (emailOrIdOnly) {
            pets = petsMatchingEmailOrId(clinic, q.trim(), viewer);
        } else {
            pets = petsRepository.searchByClinic(clinic.getId(), q.trim());
        }
        return pets.stream()
                .filter(p -> !isLegacyDoctorImport(p))
                .filter(p -> visitedPets == null || visitedPets.contains(p.getUuid()))
                .map(p -> toAppointmentPetModel(clinic, p))
                .sorted(Comparator.comparing(ClinicPetListModel::lastVisit, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional
    public PaginationModel<ClinicPetListModel> pagePets(String clinicUuid, String q, String email,
            boolean emailOrIdOnly, Integer pageNumber, Integer pageSize) {
        return PaginationSupport.slice(listPets(clinicUuid, q, email, emailOrIdOnly), pageNumber, pageSize);
    }

    /**
     * Soft-hide pets that were bulk-copied from a doctor's other clinics / personal roster.
     * Clinic CRM must only show clients registered or walked-in at THIS clinic.
     */
    private void hideLegacyDoctorImportPets(Clinic clinic) {
        for (Pet pet : petsRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            if (!isLegacyDoctorImport(pet)) {
                continue;
            }
            pet.setIsActive(false);
            petsRepository.save(pet);
        }
    }

    private static boolean isLegacyDoctorImport(Pet pet) {
        String pn = pet.getPatientNumber();
        return pn != null && pn.startsWith("doc:");
    }

    private boolean rosterVisitScoped(Clinic clinic, User viewer) {
        if (viewer == null) {
            return true;
        }
        DoctorProfile viewerDoctor = doctorProfileDao.findByUserId(viewer.getId());
        if (viewerDoctor != null && ParentBookingEnrollmentService.isPersonalPractice(clinic, viewerDoctor)) {
            return true;
        }
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(viewer.getId());
        boolean staff = clinicStaffDao.isActiveMember(clinic.getId(), viewer.getId());
        return !owner && !staff;
    }

    private Set<String> visitedPetUuids(Clinic clinic) {
        Set<VisitStatus> seen = EnumSet.of(
                VisitStatus.IN_PROGRESS, VisitStatus.CHECKING_OUT, VisitStatus.COMPLETED);
        Set<String> uuids = new HashSet<>();
        for (Visit visit : visitDao.findByClinicAndStatuses(clinic.getId(), seen)) {
            if (visit.getPet() != null) {
                uuids.add(visit.getPet().getUuid());
            }
        }
        return uuids;
    }

    /**
     * Add-appointment search: public pet ID (e.g. 6UP32B), owner email, or owner UUID.
     * Includes pets whose home clinic is elsewhere but who are enrolled here or on the
     * doctor's personal roster.
     */
    private List<Pet> petsMatchingEmailOrId(Clinic clinic, String q, User viewer) {
        String query = q.replace("%", "").replace("_", "").trim();
        if (query.length() < 3) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        LinkedHashMap<String, Pet> found = new LinkedHashMap<>();

        petsRepository.findByUuidIgnoreCase(query).ifPresent(pet -> {
            if (petVisibleForAppointmentSearch(clinic, pet, viewer)) {
                found.put(pet.getUuid(), pet);
            }
        });
        for (Pet pet : petsRepository.searchByClinicEmailOrId(clinic.getId(), query)) {
            found.putIfAbsent(pet.getUuid(), pet);
        }
        for (ClinicPetEnrollment enrollment : clinicPetEnrollmentRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            Pet pet = enrollment.getPet();
            if (pet != null && appointmentIdMatches(needle, pet, enrollment.getClinicOwner())) {
                found.putIfAbsent(pet.getUuid(), pet);
            }
        }
        DoctorProfile doctor = viewer == null ? null : doctorProfileDao.findByUserId(viewer.getId());
        if (doctor != null && ParentBookingEnrollmentService.isPersonalPractice(clinic, doctor)) {
            for (DoctorPatientEnrollment enrollment : doctorPatientEnrollmentRepository
                    .findByDoctor_IdAndIsActiveTrue(doctor.getId())) {
                Pet pet = enrollment.getPet();
                if (pet != null && appointmentIdMatches(needle, pet, pet.getClinicOwner())) {
                    found.putIfAbsent(pet.getUuid(), pet);
                }
            }
        }
        for (User platformUser : userRepository.searchActiveUsersByEmailOrUuid(query, PageRequest.of(0, 20))) {
            String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(platformUser.getEmail());
            if (!containsIgnoreCase(ownerEmail, needle) && !containsIgnoreCase(platformUser.getUuid(), needle)) {
                continue;
            }
            for (Pet pet : platformPetsOf(platformUser)) {
                if (platformPetSelectableForAppointment(pet)) {
                    found.putIfAbsent(pet.getUuid(), pet);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private List<Pet> platformPetsOf(User platformUser) {
        if (platformUser == null) {
            return List.of();
        }
        LinkedHashMap<String, Pet> byUuid = new LinkedHashMap<>();
        for (Pet pet : petsRepository.findByParentUserUuid(platformUser.getUuid())) {
            if (pet != null && pet.getUuid() != null) {
                byUuid.put(pet.getUuid(), pet);
            }
        }
        if (platformUser.getPets() != null) {
            for (Pet pet : platformUser.getPets()) {
                if (pet != null && pet.getUuid() != null) {
                    byUuid.putIfAbsent(pet.getUuid(), pet);
                }
            }
        }
        return new ArrayList<>(byUuid.values());
    }

    private static boolean platformPetSelectableForAppointment(Pet pet) {
        return pet != null
                && Boolean.TRUE.equals(pet.getIsActive())
                && !Boolean.TRUE.equals(pet.getHiddenFromParent());
    }

    private boolean petVisibleForAppointmentSearch(Clinic clinic, Pet pet, User viewer) {
        if (pet == null || !Boolean.TRUE.equals(pet.getIsActive())) {
            return false;
        }
        if (pet.getClinic() != null && pet.getClinic().getId().equals(clinic.getId())) {
            return true;
        }
        if (clinicPetEnrollmentRepository.existsByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), pet.getUuid())) {
            return true;
        }
        DoctorProfile doctor = viewer == null ? null : doctorProfileDao.findByUserId(viewer.getId());
        return doctor != null
                && ParentBookingEnrollmentService.isPersonalPractice(clinic, doctor)
                && doctorPatientEnrollmentRepository.existsByDoctor_IdAndPet_UuidAndIsActiveTrue(
                        doctor.getId(), pet.getUuid());
    }

    private static boolean appointmentIdMatches(String needle, Pet pet, ClinicPetOwner owner) {
        if (containsIgnoreCase(pet.getUuid(), needle)
                || containsIgnoreCase(pet.getPatientNumber(), needle)
                || containsIgnoreCase(pet.getParentUserUuid(), needle)) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        return containsIgnoreCase(owner.getUuid(), needle) || containsIgnoreCase(owner.getEmail(), needle);
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private ClinicPetListModel toAppointmentPetModel(Clinic clinic, Pet pet) {
        if (pet.getClinicOwner() != null && pet.getClinic() != null
                && pet.getClinic().getId().equals(clinic.getId())) {
            return toPetListModel(pet, pet.getClinicOwner());
        }
        Optional<ClinicPetEnrollment> enrollment = clinicPetEnrollmentRepository
                .findByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), pet.getUuid());
        if (enrollment.isPresent() && enrollment.get().getClinicOwner() != null) {
            return toPetListModel(pet, enrollment.get().getClinicOwner());
        }
        if (pet.getClinicOwner() != null) {
            return toPetListModel(pet, pet.getClinicOwner());
        }
        return userDao.findOptionalByPetUuid(pet.getUuid())
                .map(owner -> toPetListModel(pet, owner))
                .orElseGet(() -> new ClinicPetListModel(
                        pet.getUuid(), pet.resolveGlobalPetId(), pet.getName(), pet.getType(), pet.getBreed(),
                        pet.getGender(), pet.getDateOfBirth(), pet.getWeight(), pet.getMicrochipNumber(),
                        pet.getProfilePicture(), pet.getPatientNumber(), "", pet.getName() == null ? "" : pet.getName(),
                        null, null, false,
                        pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay()));
    }

    /** Strip internal import tags from notes shown to clinic staff. */
    private static String sanitizeOwnerNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String cleaned = notes.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("doctor:"))
                .filter(line -> !line.startsWith("Imported with doctor"))
                .filter(line -> !line.equalsIgnoreCase("Doctor as pet owner"))
                .collect(Collectors.joining("\n"))
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void ensureAffiliatedDoctorPatientsImported(Clinic clinic) {
        // Intentionally no-op: doctor personal / other-clinic patients must not mix into this clinic.
    }

    @Override
    public ClinicPetMedicalProfileModel petMedicalProfile(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        AccessiblePet access = resolveAccessiblePet(clinic, petUuid, email);
        Pet pet = access.pet();

        ClinicPetListModel petModel;
        OwnerSummaryModel ownerSummary;
        User invoiceOwner = null;
        if (access.clinicOwner() != null) {
            ClinicPetOwner owner = access.clinicOwner();
            petModel = toPetListModel(pet, owner);
            ownerSummary = ownerSummary(owner);
            invoiceOwner = owner.getLinkedUser();
        } else if (access.platformOwner() != null) {
            User owner = access.platformOwner();
            petModel = toPetListModel(pet, owner);
            ownerSummary = platformOwnerSummary(owner);
            invoiceOwner = owner;
        } else {
            throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
        }

        String medicalPetKey = pet.getUuid();
        List<HealthEventModel> timeline = healthEventsFor(clinic.getId(), medicalPetKey);
        List<VaccineScheduleModel> vaccines = petVaccineScheduleDao.findByPetUuid(medicalPetKey).stream()
                .map(this::vaccineModel).toList();

        List<BookingModel> appointments = bookingDao.findByClinic(clinic.getId()).stream()
                .filter(b -> b.getPet() != null && medicalPetKey.equals(b.getPet().getUuid()))
                .sorted(Comparator.comparing(Booking::getSlotStart, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::bookingModel)
                .toList();

        List<InvoiceSummaryModel> invoices = invoicesForMedicalProfile(clinic.getId(), medicalPetKey,
                invoiceOwner == null ? null : invoiceOwner.getId());
        List<ClinicalRecordModel> labReports = timeline.stream()
                .filter(ev -> HealthEventType.LAB_REPORT.name().equals(ev.type()))
                .map(this::clinicalRecord)
                .toList();
        List<ClinicalRecordModel> surgeries = timeline.stream()
                .filter(ev -> HealthEventType.SURGERY.name().equals(ev.type()))
                .map(this::clinicalRecord)
                .toList();

        return new ClinicPetMedicalProfileModel(petModel, ownerSummary, timeline, appointments, vaccines,
                List.of(), labReports, surgeries, invoices);
    }

    @Override
    @Transactional
    public ClinicPetListModel updatePet(String clinicUuid, String petUuid, AddOwnerPetRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        AccessiblePet access = resolveAccessiblePet(clinic, petUuid, email);
        Pet pet = access.pet();

        pet.setName(request.name().trim());
        pet.setType(blankToNull(request.species()));
        pet.setBreed(blankToNull(request.breed()));
        pet.setGender(blankToNull(request.gender()));
        pet.setDateOfBirth(request.dateOfBirth());
        pet.setWeight(blankToNull(request.weight()));
        User viewer = userDao.userByEmail(email);
        if (hasClinicInviteRole(viewer) || hasClinicStaffRole(viewer)) {
            pet.setMicrochipNumber(blankToNull(request.microchipNumber()));
        }
        pet.setProfilePicture(SafePhotoUrl.requireHttps(blankToNull(request.photoUrl())));

        Pet saved = petsRepository.save(pet);
        if (access.clinicOwner() != null) {
            return toPetListModel(saved, access.clinicOwner());
        }
        if (access.platformOwner() != null) {
            return toPetListModel(saved, access.platformOwner());
        }
        throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
    }

    @Override
    @Transactional
    public void hidePet(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        AccessiblePet access = resolveAccessiblePet(clinic, petUuid, email);
        Pet pet = access.pet();

        // Home-clinic pet: soft-hide the row. Multi-clinic / personal: only drop local membership.
        boolean homeHere = pet.getClinic() != null && pet.getClinic().getId().equals(clinic.getId());
        if (homeHere) {
            pet.setIsActive(false);
            petsRepository.save(pet);
            log.info("Soft-hid clinic pet {} at clinic {} (visits retained)", pet.getUuid(), clinicUuid);
            return;
        }
        clinicPetEnrollmentRepository.findByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), pet.getUuid())
                .ifPresent(enr -> {
                    enr.setIsActive(false);
                    clinicPetEnrollmentRepository.save(enr);
                });
        User viewer = userDao.userByEmail(email);
        DoctorProfile doctor = doctorProfileDao.findByUserId(viewer.getId());
        if (doctor != null && clinic.getOwner() != null && clinic.getOwner().getId().equals(viewer.getId())) {
            doctorPatientEnrollmentRepository.findByDoctor_IdAndPet_UuidAndIsActiveTrue(doctor.getId(), pet.getUuid())
                    .ifPresent(enr -> {
                        enr.setIsActive(false);
                        doctorPatientEnrollmentRepository.save(enr);
                    });
        }
        log.info("Removed pet {} membership at clinic {} (home clinic unchanged)", petUuid, clinicUuid);
    }

    @Override
    @Transactional
    public void hideOwner(String clinicUuid, String ownerUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        ClinicPetOwner owner = clinicPetOwnerRepository.findByUuidAndClinic_IdAndIsActiveTrue(ownerUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("owner", "uuid", ownerUuid));
        owner.setIsActive(false);
        clinicPetOwnerRepository.save(owner);
        for (Pet pet : petsRepository.findByClinicOwner_IdAndIsActiveTrue(owner.getId())) {
            pet.setIsActive(false);
            petsRepository.save(pet);
        }
        log.info("Soft-hid clinic owner {} (+pets) at clinic {} (data retained)", ownerUuid, clinicUuid);
    }

    private InvoiceSummaryModel invoiceSummary(ConsultationInvoice inv) {
        return new InvoiceSummaryModel(inv.getUuid(), inv.getStatus() == null ? null : inv.getStatus().name(),
                inv.getAmount() == null ? null : inv.getAmount().toPlainString(), inv.getCurrency(), inv.getPetUuid(),
                inv.getCreatedAt());
    }

    List<InvoiceSummaryModel> invoicesForMedicalProfile(Long clinicId, String petUuid, Long ownerUserId) {
        LinkedHashMap<String, InvoiceSummaryModel> byUuid = new LinkedHashMap<>();
        for (ConsultationInvoice inv : consultationInvoiceRepository.findAllByPetUuidOrderByCreatedAtDesc(petUuid)) {
            if (inv.getClinic() != null && clinicId.equals(inv.getClinic().getId())) {
                byUuid.put(inv.getUuid(), invoiceSummary(inv));
            }
        }
        if (ownerUserId != null) {
            for (ConsultationInvoice inv : consultationInvoiceRepository
                    .findAllByOwner_IdAndClinic_IdOrderByCreatedAtDesc(ownerUserId, clinicId)) {
                if (inv.getPetUuid() == null || petUuid.equals(inv.getPetUuid())) {
                    byUuid.putIfAbsent(inv.getUuid(), invoiceSummary(inv));
                }
            }
        }
        return new ArrayList<>(byUuid.values());
    }

    private ClinicalRecordModel clinicalRecord(HealthEventModel event) {
        return new ClinicalRecordModel(event.uuid(), event.title(), event.description(), event.date(),
                event.visitUuid(), event.attachments());
    }

    private String validateVisitLink(Clinic clinic, Pet pet, String visitUuid, HealthEventType type) {
        String trimmed = visitUuid == null || visitUuid.isBlank() ? null : visitUuid.trim();
        if (type == HealthEventType.LAB_REPORT && trimmed == null) {
            List<Visit> visits = visitDao.findByPetAndClinic(pet.getUuid(), clinic.getId());
            if (!visits.isEmpty()) {
                throw new CustomException("Select the visit this lab report belongs to.", HttpStatus.BAD_REQUEST);
            }
        }
        if (trimmed == null) {
            return null;
        }
        Visit visit = visitDao.findByUuidAndClinicId(trimmed, clinic.getId())
                .orElseThrow(() -> new CustomException("Visit not found at this clinic.", HttpStatus.NOT_FOUND));
        if (visit.getPet() == null || !pet.getUuid().equals(visit.getPet().getUuid())) {
            throw new CustomException("Visit does not belong to this pet.", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private Pet saveClinicPet(Clinic clinic, ClinicPetOwner owner, String name, String species, String breed,
            String gender, LocalDate dob, String weight, String microchip, String photoUrl, String patientNumber) {
        Pet pet = Pet.builder()
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
        List<Pet> pets = new ArrayList<>(petsRepository.findByClinicOwner_IdAndIsActiveTrue(owner.getId()).stream()
                .filter(p -> !isLegacyDoctorImport(p))
                .toList());
        Set<String> seenPetUuids = pets.stream().map(Pet::getUuid).collect(Collectors.toSet());
        if (owner.getLinkedUser() != null) {
            for (Pet pet : platformPetsOf(owner.getLinkedUser())) {
                if (!seenPetUuids.contains(pet.getUuid()) && platformPetSelectableForAppointment(pet)) {
                    pets.add(pet);
                    seenPetUuids.add(pet.getUuid());
                }
            }
        }
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
                owner.getAddress(), sanitizeOwnerNotes(owner.getNotes()), linked, linkedUuid, petModels.size(), lastVisit,
                petModels);
    }

    private ClinicPetListModel toPetListModel(Pet pet) {
        return toPetListModel(pet, pet.getClinicOwner());
    }

    private ClinicPetListModel toPetListModel(Pet pet, ClinicPetOwner owner) {
        return new ClinicPetListModel(pet.getUuid(), pet.resolveGlobalPetId(), pet.getName(), pet.getType(),
                pet.getBreed(), pet.getGender(), pet.getDateOfBirth(), pet.getWeight(), pet.getMicrochipNumber(),
                pet.getProfilePicture(), pet.getPatientNumber(), owner.getUuid(), clinicOwnerName(owner),
                formatClinicPhone(owner.getPhone()), owner.getEmail(), owner.getLinkedUser() != null,
                pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay());
    }

    private ClinicPetListModel toPetListModel(Pet pet, User owner) {
        return new ClinicPetListModel(pet.getUuid(), pet.resolveGlobalPetId(), pet.getName(), pet.getType(),
                pet.getBreed(), pet.getGender(), pet.getDateOfBirth(), pet.getWeight(), pet.getMicrochipNumber(),
                pet.getProfilePicture(), pet.getPatientNumber(), owner.getUuid(), fullName(owner),
                formatPhone(owner), owner.getEmail(), true,
                pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay());
    }

    private OwnerSummaryModel ownerSummary(ClinicPetOwner owner) {
        boolean linked = owner.getLinkedUser() != null;
        return new OwnerSummaryModel(owner.getUuid(), clinicOwnerName(owner), owner.getEmail(),
                formatClinicPhone(owner.getPhone()), owner.getAddress(), linked,
                linked ? owner.getLinkedUser().getUuid() : null);
    }

    private OwnerSummaryModel platformOwnerSummary(User owner) {
        return new OwnerSummaryModel(owner.getUuid(), fullName(owner), owner.getEmail(), formatPhone(owner),
                primaryAddress(owner), true, owner.getUuid());
    }

    private PatientDetailModel platformOwnerPatientDetail(Clinic clinic, Pet selected, User owner) {
        PatientModel patient = patientModelFromPlatformOwner(selected, owner);
        OwnerSummaryModel ownerSummary = platformOwnerSummary(owner);
        List<PatientPetModel> pets = List.of(new PatientPetModel(selected.getUuid(), selected.getName(),
                selected.getType(), selected.getBreed(),
                selected.getRegisteredAt() == null ? null : selected.getRegisteredAt().atStartOfDay(), true,
                selected.resolveGlobalPetId(), selected.getMicrochipNumber()));
        List<HealthEventModel> events = healthEventsFor(clinic.getId(), selected.getUuid());
        List<VaccineScheduleModel> vaccines = petVaccineScheduleDao.findByPetUuid(selected.getUuid()).stream()
                .map(this::vaccineModel).toList();
        return new PatientDetailModel(patient, ownerSummary, pets, events, vaccines);
    }

    private PatientModel patientModelFromPlatformOwner(Pet pet, User owner) {
        return new PatientModel(pet.getUuid(), pet.getName(), fullName(owner), owner.getEmail(),
                pet.getRegisteredAt() == null ? null : pet.getRegisteredAt().atStartOfDay(), owner.getUuid(),
                formatPhone(owner), primaryAddress(owner), pet.getType(), pet.getBreed());
    }

    /**
     * Pet is accessible at this clinic if home-registered, multi-clinic enrolled, or
     * (personal practice) on the viewing doctor's patient roster — without moving clinic_id.
     * {@code petUuid} may be the public 6-character id or a clinic patient number.
     */
    private AccessiblePet resolveAccessiblePet(Clinic clinic, String petUuid, String email) {
        String key = petUuid == null ? "" : petUuid.trim();
        Pet resolved = petsRepository.findByUuidIgnoreCase(key).orElse(null);
        if (resolved == null) {
            resolved = petsRepository.findFirstByClinic_IdAndPatientNumberIgnoreCase(clinic.getId(), key).orElse(null);
        }
        String id = resolved != null ? resolved.getUuid() : key;

        Optional<Pet> home = petsRepository.findByUuidAndClinic_Id(id, clinic.getId());
        if (home.isPresent()) {
            Pet pet = home.get();
            if (pet.getClinicOwner() != null) {
                return new AccessiblePet(pet, pet.getClinicOwner(),
                        pet.getClinicOwner().getLinkedUser() != null ? pet.getClinicOwner().getLinkedUser() : null);
            }
        }

        Optional<ClinicPetEnrollment> enrollment = clinicPetEnrollmentRepository
                .findByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), id);
        if (enrollment.isPresent()) {
            ClinicPetEnrollment enr = enrollment.get();
            User linked = enr.getClinicOwner() != null ? enr.getClinicOwner().getLinkedUser() : null;
            return new AccessiblePet(enr.getPet(), enr.getClinicOwner(), linked);
        }

        User viewer = userDao.userByEmail(email);
        if (clinic.getOwner() != null && clinic.getOwner().getId().equals(viewer.getId())) {
            DoctorProfile doctor = doctorProfileDao.findByUserId(viewer.getId());
            if (doctor != null) {
                Optional<DoctorPatientEnrollment> docEnr = doctorPatientEnrollmentRepository
                        .findByDoctor_IdAndPet_UuidAndIsActiveTrue(doctor.getId(), id);
                if (docEnr.isPresent()) {
                    DoctorPatientEnrollment enr = docEnr.get();
                    return new AccessiblePet(enr.getPet(), null, enr.getOwnerUser());
                }
            }
        }

        if (patientMap(clinic).containsKey(id)) {
            Pet pet = requirePet(id);
            User owner = userDao.userByPetUuid(id);
            if (pet.getClinicOwner() != null && pet.getClinic() != null
                    && pet.getClinic().getId().equals(clinic.getId())) {
                return new AccessiblePet(pet, pet.getClinicOwner(), owner);
            }
            return new AccessiblePet(pet, null, owner);
        }

        throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
    }

    private record AccessiblePet(Pet pet, ClinicPetOwner clinicOwner, User platformOwner) {
    }

    private PatientDetailModel clinicRegisteredPatientDetail(Clinic clinic, Pet selected) {
        return clinicRegisteredPatientDetail(clinic, selected, selected.getClinicOwner());
    }

    private PatientDetailModel clinicRegisteredPatientDetail(Clinic clinic, Pet selected, ClinicPetOwner owner) {
        PatientModel patient = toClinicPatientModel(selected, owner);
        OwnerSummaryModel ownerSummary = ownerSummary(owner);

        Map<String, PatientPetModel> petsByUuid = new HashMap<>();
        for (Pet p : petsRepository.findByClinicOwner_UuidAndIsActiveTrue(owner.getUuid())) {
            boolean atClinic = (p.getClinic() != null && p.getClinic().getId().equals(clinic.getId()))
                    || clinicPetEnrollmentRepository.existsByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(),
                            p.getUuid());
            if (!atClinic) {
                continue;
            }
            petsByUuid.put(p.getUuid(), new PatientPetModel(p.getUuid(), p.getName(), p.getType(), p.getBreed(),
                    p.getRegisteredAt() == null ? null : p.getRegisteredAt().atStartOfDay(), true,
                    p.resolveGlobalPetId(), p.getMicrochipNumber()));
        }
        // Include enrolled pets for this owner even when clinic_owner_id on pet points elsewhere.
        for (ClinicPetEnrollment enrollment : clinicPetEnrollmentRepository.findByClinic_IdAndIsActiveTrue(clinic.getId())) {
            if (enrollment.getClinicOwner() == null || !enrollment.getClinicOwner().getId().equals(owner.getId())) {
                continue;
            }
            Pet p = enrollment.getPet();
            if (p == null || !Boolean.TRUE.equals(p.getIsActive())) {
                continue;
            }
            petsByUuid.putIfAbsent(p.getUuid(),
                    new PatientPetModel(p.getUuid(), p.getName(), p.getType(), p.getBreed(),
                            p.getRegisteredAt() == null ? null : p.getRegisteredAt().atStartOfDay(), true,
                            p.resolveGlobalPetId(), p.getMicrochipNumber()));
        }
        List<PatientPetModel> pets = petsByUuid.values().stream()
                .sorted(Comparator.comparing(PatientPetModel::petName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        String medicalKey = selected.getUuid();
        List<HealthEventModel> events = healthEventsFor(clinic.getId(), medicalKey);
        List<VaccineScheduleModel> vaccines = petVaccineScheduleDao.findByPetUuid(medicalKey).stream()
                .map(this::vaccineModel).toList();

        return new PatientDetailModel(patient, ownerSummary, pets, events, vaccines);
    }

    private PatientModel toClinicPatientModel(Pet pet) {
        return toClinicPatientModel(pet, pet.getClinicOwner());
    }

    private PatientModel toClinicPatientModel(Pet pet, ClinicPetOwner owner) {
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

    @Override
    public PaginationModel<BookingModel> bookings(String clinicUuid, String status, int page, int size, String email) {
        Clinic clinic = access(clinicUuid, email);
        BookingStatus bookingStatus = status == null || status.isBlank() ? null : BookingStatus.valueOf(status.toUpperCase());
        Page<Booking> bookings = bookingDao.findByClinic(clinic.getId(), bookingStatus,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "slotStart")));
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
        User owner = resolveNotifiableOwner(pet);
        if (owner == null) {
            throw new CustomException("Owner has no KittyP account to notify — share the alert manually",
                    HttpStatus.BAD_REQUEST);
        }
        notificationLogRepository.save(NotificationLog.builder().user(owner).pet(pet).type(NotificationType.VACCINATION_DUE)
                .payload(alert.message()).sentAt(LocalDateTime.now()).build());
    }

    @Override
    public List<HealthEventModel> healthEvents(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requirePatient(clinic, petUuid, email);
        return healthEventsFor(clinic.getId(), petUuid);
    }

    @Override
    @Transactional
    public HealthEventModel createHealthEvent(String clinicUuid, String petUuid, HealthEventRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        Pet pet = resolveAccessiblePet(clinic, petUuid, email).pet();
        String visitUuid = validateVisitLink(clinic, pet, request.visitUuid(), request.type());
        HealthEvent event = HealthEvent.builder().uuid(UUID.randomUUID().toString()).clinic(clinic).pet(pet)
                .type(request.type()).title(request.title()).description(request.description()).date(request.date())
                .isPast(request.isPast()).status(request.status()).attachments(request.attachments())
                .visitUuid(visitUuid).build();
        return healthEventModel(healthEventDao.save(event));
    }

    @Override
    @Transactional
    public HealthEventModel createClinicalRecord(String clinicUuid, String petUuid, ClinicalRecordRequest request,
            String email) {
        if (request.type() != HealthEventType.LAB_REPORT && request.type() != HealthEventType.SURGERY) {
            throw new CustomException("Record type must be LAB_REPORT or SURGERY", HttpStatus.BAD_REQUEST);
        }
        return createHealthEvent(clinicUuid, petUuid,
                new HealthEventRequest(request.type(), request.title(), request.description(), request.date(), true,
                        HealthEventStatus.COMPLETED, request.attachments(), request.visitUuid()),
                email);
    }

    @Override
    public void assertClinicalUploadAllowed(String clinicUuid, String petUuid, String visitUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        Pet pet = resolveAccessiblePet(clinic, petUuid, email).pet();
        validateVisitLink(clinic, pet, visitUuid, null);
    }

    @Override
    public List<VaccineCatalogModel> vaccineCatalog(String clinicUuid, String species, String email) {
        access(clinicUuid, email);
        String speciesFilter = species == null ? "" : species.trim();
        List<VaccineCatalogModel> all = vaccineMasterRepository.findAll().stream()
                .map(v -> new VaccineCatalogModel(v.getId(), v.getName(), v.getSpecies(), v.getDescription()))
                .toList();
        if (speciesFilter.isBlank()) {
            return all;
        }
        List<VaccineCatalogModel> filtered = all.stream()
                .filter(v -> v.species() == null || v.species().isBlank() || v.species().equalsIgnoreCase(speciesFilter))
                .toList();
        return filtered.isEmpty() ? all : filtered;
    }

    @Override
    @Transactional
    public VaccineScheduleModel addVaccineDue(String clinicUuid, String petUuid, AddVaccineDueRequest request,
            String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        Pet pet = resolveAccessiblePet(clinic, petUuid, email).pet();
        VaccineMaster vaccine = vaccineMasterRepository.findById(request.vaccineMasterId())
                .orElseThrow(() -> new ResourceNotFoundException("vaccine", "id", String.valueOf(request.vaccineMasterId())));
        PetVaccineSchedule schedule = PetVaccineSchedule.builder()
                .pet(pet)
                .vaccine(vaccine)
                .dueDate(request.dueDate())
                .completed(false)
                .build();
        return vaccineModel(petVaccineScheduleDao.save(schedule));
    }

    @Override
    @Transactional
    public VaccineScheduleModel markVaccineGiven(String clinicUuid, String petUuid, Long scheduleId,
            MarkVaccineGivenRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        Pet pet = resolveAccessiblePet(clinic, petUuid, email).pet();
        PetVaccineSchedule schedule = petVaccineScheduleDao.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("vaccine schedule", "id", String.valueOf(scheduleId)));
        if (schedule.getPet() == null || !pet.getUuid().equals(schedule.getPet().getUuid())) {
            throw new CustomException("Vaccine schedule does not belong to this pet.", HttpStatus.NOT_FOUND);
        }
        schedule.setCompleted(true);
        schedule.setCompletedDate(request != null && request.completedDate() != null
                ? request.completedDate()
                : LocalDate.now());
        if (request != null && request.certificateUrl() != null && !request.certificateUrl().isBlank()) {
            schedule.setCertificateUrl(request.certificateUrl().trim());
        }
        return vaccineModel(petVaccineScheduleDao.save(schedule));
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

    /**
     * Document URLs are tenant-isolated: the doctor themselves, or a clinic admin of
     * this clinic (owner or staff with ROLE_CLINIC_ADMIN). Staff-only and
     * cross-tenant admins affiliated only as doctors do not receive URLs.
     */
    private boolean canViewDoctorCertificates(Clinic clinic, User viewer, DoctorProfile doctor) {
        if (viewer == null || doctor == null) {
            return false;
        }
        User doctorUser = doctor.getUser();
        if (doctorUser != null && doctorUser.getId() != null && doctorUser.getId().equals(viewer.getId())) {
            return true;
        }
        if (!hasClinicAdminRole(viewer)) {
            return false;
        }
        if (clinic.getOwner() != null && clinic.getOwner().getId() != null
                && clinic.getOwner().getId().equals(viewer.getId())) {
            return true;
        }
        return clinicStaffDao.isActiveMember(clinic.getId(), viewer.getId());
    }

    private static boolean hasClinicAdminRole(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return false;
        }
        return user.getUserRoles().stream().anyMatch(userRole -> {
            ERole role = userRole.getRole() == null ? null : userRole.getRole().getName();
            return CLINIC_ADMIN_ROLE.equals(role);
        });
    }

    private static boolean hasClinicStaffRole(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return false;
        }
        return user.getUserRoles().stream().anyMatch(userRole -> {
            ERole role = userRole.getRole() == null ? null : userRole.getRole().getName();
            return ERole.ROLE_CLINIC_STAFF.equals(role);
        });
    }

    private void requireOperational(Clinic clinic) {
        if (clinic.getStatus() == ClinicStatus.SHUTDOWN) {
            throw new CustomException("This clinic is shut down and is read-only.", HttpStatus.BAD_REQUEST);
        }
    }

    private void requireActivated(Clinic clinic) {
        requireOperational(clinic);
        if (isOwnerAffiliatedDoctor(clinic)) {
            return;
        }
        if (clinic.getStatus() == null || !clinic.getStatus().isActivated()) {
            throw new CustomException(ClinicStatus.NOT_ACTIVATED_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private void requirePracticeReady(DoctorProfile doctor) {
        if (doctor == null) {
            return;
        }
        DoctorStatus status = doctor.getStatus();
        if (status == null || !status.isPracticeReady()) {
            throw new CustomException(DoctorStatus.PRACTICE_NOT_READY_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private void requireCanInviteDoctors(User user) {
        if (!hasClinicInviteRole(user)) {
            throw new AccessDeniedException("Only clinic admins can invite doctors");
        }
    }

    private static boolean hasClinicInviteRole(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return false;
        }
        return user.getUserRoles().stream().anyMatch(userRole -> {
            ERole role = userRole.getRole() == null ? null : userRole.getRole().getName();
            return ERole.ROLE_CLINIC_ADMIN.equals(role) || ERole.ROLE_ADMIN.equals(role);
        });
    }

    private void requireClinicManager(Clinic clinic, User user) {
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(user.getId());
        if (owner) {
            return;
        }
        boolean clinicAdmin = user.getUserRoles().stream()
                .anyMatch(userRole -> CLINIC_ADMIN_ROLE.equals(userRole.getRole().getName()));
        boolean doctorHere = clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(),
                user.getId());
        if (clinicAdmin && doctorHere) {
            return;
        }
        throw new CustomException("You do not have permission to manage this clinic",
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
        Map<String, PatientModel> result = new HashMap<>();
        for (String uuid : petUuids) {
            Pet pet = requirePet(uuid);
            result.put(uuid, patientModelForPet(pet, lastVisits.get(uuid)));
        }
        return result;
    }

    /** Platform parent pets or clinic-registered pets (no KittyP user required). */
    private PatientModel patientModelForPet(Pet pet, LocalDateTime lastVisit) {
        if (pet.getClinicOwner() != null) {
            return withLastVisit(toClinicPatientModel(pet), lastVisit);
        }
        User owner = userDao.userByPetUuid(pet.getUuid());
        return new PatientModel(pet.getUuid(), pet.getName(), fullName(owner), owner.getEmail(), lastVisit,
                owner.getUuid(), formatPhone(owner), primaryAddress(owner), pet.getType(), pet.getBreed());
    }

    private PatientModel withLastVisit(PatientModel model, LocalDateTime lastVisit) {
        LocalDateTime last = laterOf(model.lastVisit(), lastVisit);
        if (Objects.equals(last, model.lastVisit())) {
            return model;
        }
        return new PatientModel(model.petUuid(), model.petName(), model.ownerName(), model.ownerEmail(), last,
                model.ownerUuid(), model.ownerPhone(), model.ownerAddress(), model.species(), model.breed());
    }

    private User resolveNotifiableOwner(Pet pet) {
        if (pet.getClinicOwner() != null && pet.getClinicOwner().getLinkedUser() != null) {
            return pet.getClinicOwner().getLinkedUser();
        }
        if (pet.getClinicOwner() != null) {
            return null;
        }
        return userDao.userByPetUuid(pet.getUuid());
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

    private PatientModel requirePatient(Clinic clinic, String petUuid, String email) {
        AccessiblePet access = resolveAccessiblePet(clinic, petUuid, email);
        if (access.clinicOwner() != null) {
            return toClinicPatientModel(access.pet(), access.clinicOwner());
        }
        if (access.platformOwner() != null) {
            return patientModelFromPlatformOwner(access.pet(), access.platformOwner());
        }
        throw new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND);
    }

    private Pet requirePet(String petUuid) {
        Pet pet = petDao.petByUuid(petUuid);
        if (pet == null) {
            throw new ResourceNotFoundException("pet", "uuid", petUuid);
        }
        return pet;
    }

    private ClinicModel clinicModel(Clinic clinic) {
        return clinicModel(clinic, null);
    }

    private ClinicModel clinicModel(Clinic clinic, User viewer) {
        boolean personal = viewer != null
                ? isViewerPersonalPractice(clinic, viewer)
                : isOwnerAffiliatedDoctor(clinic);
        boolean waConfigured = WhatsAppSettingsSupport.isConfigured(
                clinic.getWhatsappPhoneNumberId(),
                clinic.getWhatsappBusinessAccountId(),
                whatsappTokenOrNull(clinic));
        String status = clinic.getStatus() == null ? ClinicStatus.PENDING.name() : clinic.getStatus().name();
        return new ClinicModel(clinic.getUuid(), clinic.getName(), clinic.getLicenseNumber(), clinic.getAddress(),
                clinic.getPhone(), clinic.getEmail(), clinic.getTimezone(), clinic.getOperatingHours(),
                status, personal, waConfigured,
                clinic.getCity(), clinic.getLatitude(), clinic.getLongitude(), clinic.getProfileImageUrl());
    }

    private static String whatsappTokenOrNull(Clinic clinic) {
        try {
            return clinic.getWhatsappToken();
        } catch (RuntimeException ex) {
            log.warn("Could not read WhatsApp token for clinic {}: {}", clinic.getUuid(), ex.getMessage());
            return null;
        }
    }

    private Clinic ensurePersonalPractice(User user, DoctorProfile profile) {
        if (user == null || profile == null || user.getId() == null) {
            return null;
        }
        for (Clinic owned : clinicDao.findAllByOwnerUserId(user.getId())) {
            if (owned.getId() != null && clinicDoctorRepository
                    .existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(owned.getId(), user.getId())) {
                if (profile.getClinic() == null) {
                    profile.setClinic(owned);
                    doctorProfileDao.save(profile);
                }
                return owned;
            }
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        String name = full.isEmpty() ? "Personal practice" : "Dr. " + full;
        Clinic personal = clinicDao.saveClinic(Clinic.builder()
                .name(name)
                .email(user.getEmail())
                .phone(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : user.getPhoneNumber())
                .owner(user)
                .status(ClinicStatus.VERIFIED)
                .build());
        clinicDoctorRepository.save(ClinicDoctor.builder()
                .clinic(personal)
                .doctor(profile)
                .role("owner")
                .isActive(true)
                .joinedAt(LocalDate.now())
                .build());
        if (profile.getClinic() == null) {
            profile.setClinic(personal);
            doctorProfileDao.save(profile);
        }
        return personal;
    }

    private static boolean hasClinicManagerRole(User user) {
        if (user == null || user.getUserRoles() == null) {
            return false;
        }
        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .anyMatch(role -> role == ERole.ROLE_CLINIC_ADMIN);
    }

    /** Viewer-owned solo practice. Invited doctors on someone else's clinic are not personal. */
    private boolean isViewerPersonalPractice(Clinic clinic, User viewer) {
        if (clinic == null || viewer == null || viewer.getId() == null) {
            return false;
        }
        if (clinic.getOwner() == null || clinic.getOwner().getId() == null) {
            return false;
        }
        if (!clinic.getOwner().getId().equals(viewer.getId())) {
            return false;
        }
        return isOwnerAffiliatedDoctor(clinic);
    }

    /** Solo doctor practice: owner has a doctor profile or an active clinic affiliation. */
    private boolean isOwnerAffiliatedDoctor(Clinic clinic) {
        Long ownerUserId = resolveOwnerUserId(clinic);
        if (ownerUserId == null) {
            return false;
        }
        if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(
                clinic.getId(), ownerUserId)) {
            return true;
        }
        return doctorProfileDao.findByUserId(ownerUserId) != null;
    }

    private Long resolveOwnerUserId(Clinic clinic) {
        if (clinic == null || clinic.getId() == null) {
            return null;
        }
        try {
            if (clinic.getOwner() != null && clinic.getOwner().getId() != null) {
                return clinic.getOwner().getId();
            }
        } catch (RuntimeException ex) {
            log.warn("Could not read clinic owner for {}: {}", clinic.getUuid(), ex.getMessage());
        }
        return clinicDao.findOwnerUserId(clinic.getId());
    }

    private DoctorModel doctorModel(ClinicDoctor affiliation) {
        var doctor = affiliation.getDoctor();
        Double rating = doctor.getRating();
        Integer reviews = doctor.getReviewsCount();
        return new DoctorModel(doctor.getUuid(), doctor.getUser().getUuid(), fullName(doctor.getUser()),
                doctor.getUser().getEmail(), doctor.getSpecialization() == null ? null : doctor.getSpecialization().name(),
                affiliation.getRole(), affiliation.getIsActive(),
                doctor.getStatus() == null ? null : doctor.getStatus().name(), doctor.getPhotoUrl(),
                rating, reviews, ratingLabel(rating),
                affiliation.getJoinedAt() == null ? null : affiliation.getJoinedAt().toString(),
                doctor.getExperienceYears(),
                doctor.getRegistrationNumber(),
                affiliation.getClinic() == null ? null : affiliation.getClinic().getUuid());
    }

    private static String ratingLabel(Double rating) {
        if (rating == null || rating <= 0) {
            return "Not rated yet";
        }
        int n = (int) Math.round(rating);
        if (n <= 1) {
            return "Still warming up";
        }
        if (n == 2) {
            return "Gentle paws";
        }
        if (n == 3) {
            return "Trusted companion";
        }
        if (n == 4) {
            return "Clinic favorite";
        }
        return "Legend of care";
    }

    private BookingModel bookingModel(Booking booking) {
        String ownerName = booking.getOwner() == null ? null : fullName(booking.getOwner());
        if ((ownerName == null || ownerName.isBlank()) && booking.getPet() != null
                && booking.getPet().getClinicOwner() != null) {
            ClinicPetOwner co = booking.getPet().getClinicOwner();
            String last = co.getLastName() == null ? "" : co.getLastName().trim();
            ownerName = (co.getFirstName() + (last.isEmpty() ? "" : " " + last)).trim();
        }
        return new BookingModel(booking.getUuid(), booking.getPet().getUuid(), booking.getPet().getName(),
                ownerName, booking.getDoctor() == null ? null : booking.getDoctor().getUuid(),
                booking.getSlotStart(), booking.getSlotEnd(), booking.getTimezone(), booking.getStatus(),
                booking.getMode() == null ? null : booking.getMode().name(), booking.getNotes(),
                booking.getClinic() == null ? null : booking.getClinic().getUuid(),
                booking.getClinic() == null ? null : booking.getClinic().getName(),
                doctorDisplayName(booking.getDoctor()),
                booking.getDoctor() == null || booking.getDoctor().getSpecialization() == null ? null
                        : booking.getDoctor().getSpecialization().name(),
                booking.getDoctor() == null ? null : booking.getDoctor().getPhotoUrl(),
                booking.getPet() == null ? null : booking.getPet().getType(),
                booking.getVideoJoinUrl());
    }

    private static String doctorDisplayName(com.kittyp.doctor.entity.DoctorProfile doctor) {
        if (doctor == null || doctor.getUser() == null) {
            return null;
        }
        String name = ((doctor.getUser().getFirstName() == null ? "" : doctor.getUser().getFirstName()) + " "
                + (doctor.getUser().getLastName() == null ? "" : doctor.getUser().getLastName())).trim();
        return name.isBlank() ? null : name;
    }

    private List<HealthEventModel> healthEventsFor(Long clinicId, String petUuid) {
        return healthEventDao.findByClinicAndPet(clinicId, petUuid).stream().map(this::healthEventModel).toList();
    }

    private HealthEventModel healthEventModel(HealthEvent event) {
        return new HealthEventModel(event.getUuid(), event.getType().name(), event.getTitle(), event.getDescription(),
                event.getDate(), event.getIsPast(), event.getStatus() == null ? null : event.getStatus().name(),
                event.getAttachments(), event.getVisitUuid());
    }

    private VaccineScheduleModel vaccineModel(PetVaccineSchedule schedule) {
        return new VaccineScheduleModel(schedule.getId(), schedule.getVaccine().getName(), schedule.getDueDate(),
                schedule.getCompleted(), schedule.getCompletedDate(), schedule.getCertificateUrl());
    }

    private RetentionAlertModel vaccineAlert(PetVaccineSchedule schedule, LocalDate today) {
        Pet pet = schedule.getPet();
        String ownerName;
        if (pet.getClinicOwner() != null) {
            ownerName = clinicOwnerName(pet.getClinicOwner());
        } else {
            ownerName = fullName(userDao.userByPetUuid(pet.getUuid()));
        }
        long days = ChronoUnit.DAYS.between(today, schedule.getDueDate());
        String status = days < 0 ? "OVERDUE" : "DUE_SOON";
        String message = days < 0 ? schedule.getVaccine().getName() + " vaccine is overdue."
                : schedule.getVaccine().getName() + " vaccine is due in " + days + " days.";
        return new RetentionAlertModel("vaccine-" + schedule.getId(), pet.getUuid(), pet.getName(), ownerName,
                "VACCINE", message, days, status);
    }

    private static LocalDateTime laterOf(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private String fullName(User user) {
        return String.join(" ", user.getFirstName() == null ? "" : user.getFirstName(),
                user.getLastName() == null ? "" : user.getLastName()).trim();
    }
}
