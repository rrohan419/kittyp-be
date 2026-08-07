package com.kittyp.visit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.booking.enums.BookingMode;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.health.entity.HealthEvent;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.dto.VisitDtos.AttendedPatientModel;
import com.kittyp.visit.dto.VisitDtos.VisitChartModel;
import com.kittyp.visit.dto.VisitDtos.VisitChartRequest;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.dto.VisitDtos.VisitPatchRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInOwnerRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInPetRequest;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private static final Set<VisitStatus> ASSIGNABLE = EnumSet.of(
            VisitStatus.WAITLIST, VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS, VisitStatus.CHECKING_OUT);
    private static final Set<VisitStatus> STARTABLE = EnumSet.of(
            VisitStatus.WAITLIST, VisitStatus.CHECKED_IN);
    /** Doctor finish treatment → checkout (not final COMPLETED). */
    private static final Set<VisitStatus> FINISHABLE = EnumSet.of(VisitStatus.IN_PROGRESS);

    private final VisitDao visitDao;
    private final ClinicDao clinicDao;
    private final ClinicStaffDao clinicStaffDao;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicPetOwnerRepository clinicPetOwnerRepository;
    private final PetsRepository petsRepository;
    private final DoctorProfileDao doctorProfileDao;
    private final UserDao userDao;
    private final ClinicOwnerUserLinkService clinicOwnerUserLinkService;
    private final HealthEventDao healthEventDao;
    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public VisitModel createWalkIn(String clinicUuid, WalkInCreateRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);

        Pet pet = resolvePetForWalkIn(clinic, request);
        ClinicPetOwner owner = pet.getClinicOwner();

        DoctorProfile doctor = null;
        if (request.doctorUuid() != null && !request.doctorUuid().isBlank()) {
            doctor = requireClinicDoctor(clinic, request.doctorUuid());
        }

        Visit visit = Visit.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(clinic)
                .pet(pet)
                .clinicOwner(owner)
                .doctor(doctor)
                .source(VisitSource.WALK_IN)
                .channel(BookingMode.IN_PERSON)
                .status(VisitStatus.WAITLIST)
                .urgency(request.urgency() == null ? VisitUrgency.ROUTINE : request.urgency())
                .reasonForVisit(blankToNull(request.reasonForVisit()))
                .build();
        visit.setIsActive(true);
        visit = visitDao.save(visit);
        if (doctor != null) {
            notifyDoctorOfPatient(visit, "assigned");
        }
        return toModel(visit, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitModel> listClinicVisits(String clinicUuid, LocalDate date, VisitStatus status,
            String doctorUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        LocalDate day = date == null ? LocalDate.now() : date;
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.atTime(LocalTime.MAX);

        List<Visit> visits = status == null
                ? visitDao.findByClinicAndDay(clinic.getId(), from, to)
                : visitDao.findByClinicStatusAndDay(clinic.getId(), status, from, to);

        if (doctorUuid != null && !doctorUuid.isBlank()) {
            visits = visits.stream()
                    .filter(v -> v.getDoctor() != null && doctorUuid.equals(v.getDoctor().getUuid()))
                    .toList();
        }

        return visits.stream()
                .sorted((a, b) -> {
                    int urg = Integer.compare(urgencyRank(b.getUrgency()), urgencyRank(a.getUrgency()));
                    if (urg != 0) {
                        return urg;
                    }
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .map(v -> toModel(v, true))
                .toList();
    }

    @Override
    @Transactional
    public VisitModel patchVisit(String clinicUuid, String visitUuid, VisitPatchRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);
        Visit visit = visitDao.findByUuidAndClinicId(visitUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("visit", "uuid", visitUuid));

        DoctorProfile previousDoctor = visit.getDoctor();
        boolean doctorChanged = false;

        if (request.doctorUuid() != null) {
            if (!ASSIGNABLE.contains(visit.getStatus())) {
                throw new CustomException("Cannot assign doctor in status " + visit.getStatus(), HttpStatus.BAD_REQUEST);
            }
            if (request.doctorUuid().isBlank()) {
                visit.setDoctor(null);
                doctorChanged = previousDoctor != null;
            } else {
                DoctorProfile next = requireClinicDoctor(clinic, request.doctorUuid());
                visit.setDoctor(next);
                doctorChanged = previousDoctor == null || !previousDoctor.getId().equals(next.getId());
            }
        }

        if (request.urgency() != null) {
            visit.setUrgency(request.urgency());
        }
        if (request.reasonForVisit() != null) {
            visit.setReasonForVisit(blankToNull(request.reasonForVisit()));
        }
        if (request.status() != null) {
            if (request.status() == VisitStatus.CHECKED_IN && visit.getDoctor() == null) {
                throw new CustomException("Assign a doctor before check-in so they can see this patient",
                        HttpStatus.BAD_REQUEST);
            }
            applyStatusTransition(visit, request.status());
            if (visit.getStatus() == VisitStatus.COMPLETED) {
                ensureHealthEvent(visit);
            }
            if (visit.getClinicOwner() != null
                    && (visit.getStatus() == VisitStatus.CHECKED_IN || visit.getStatus() == VisitStatus.COMPLETED
                            || visit.getStatus() == VisitStatus.CHECKING_OUT)) {
                clinicOwnerUserLinkService.linkOwnerIfUserExists(visit.getClinicOwner());
            }
        }

        visit = visitDao.save(visit);

        if (doctorChanged && visit.getDoctor() != null) {
            notifyDoctorOfPatient(visit, "assigned");
        } else if (request.status() == VisitStatus.CHECKED_IN && visit.getDoctor() != null) {
            notifyDoctorOfPatient(visit, "checked in");
        }

        return toModel(visit, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitModel> listPetVisitsForClinic(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(petUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("pet", "uuid", petUuid));
        return visitDao.findByPetAndClinic(petUuid, clinic.getId()).stream()
                .map(v -> toModel(v, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitModel> listMyDoctorVisits(LocalDate date, String email) {
        DoctorProfile profile = requireDoctorProfile(email);
        LocalDate day = date == null ? LocalDate.now() : date;
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.atTime(LocalTime.MAX);
        return visitDao.findByDoctorAndDay(profile.getId(), from, to).stream()
                .map(v -> toModel(v, true))
                .toList();
    }

    @Override
    @Transactional
    public VisitModel startVisit(String visitUuid, String email) {
        Visit visit = requireDoctorOwnedVisit(visitUuid, email);
        if (!STARTABLE.contains(visit.getStatus()) && visit.getStatus() != VisitStatus.IN_PROGRESS) {
            throw new CustomException("Cannot start visit from status " + visit.getStatus(), HttpStatus.BAD_REQUEST);
        }
        if (visit.getDoctor() == null) {
            visit.setDoctor(requireDoctorProfile(email));
        }
        if (visit.getStatus() != VisitStatus.IN_PROGRESS) {
            applyStatusTransition(visit, VisitStatus.IN_PROGRESS);
        }
        return toModel(visitDao.save(visit), true);
    }

    @Override
    @Transactional
    public VisitModel saveChart(String visitUuid, VisitChartRequest request, String email) {
        Visit visit = requireDoctorOwnedVisit(visitUuid, email);
        if (visit.getStatus() == VisitStatus.COMPLETED || visit.getStatus() == VisitStatus.CANCELLED
                || visit.getStatus() == VisitStatus.NO_SHOW) {
            throw new CustomException("Cannot edit chart for a closed visit", HttpStatus.BAD_REQUEST);
        }
        if (STARTABLE.contains(visit.getStatus())) {
            applyStatusTransition(visit, VisitStatus.IN_PROGRESS);
        }
        if (request.examinationNotes() != null) {
            visit.setExaminationNotes(blankToNull(request.examinationNotes()));
        }
        if (request.assessment() != null) {
            visit.setAssessment(blankToNull(request.assessment()));
        }
        if (request.plan() != null) {
            visit.setPlan(blankToNull(request.plan()));
        }
        if (request.nextVisitNotes() != null) {
            visit.setNextVisitNotes(blankToNull(request.nextVisitNotes()));
        }
        if (request.internalNotes() != null) {
            visit.setInternalNotes(blankToNull(request.internalNotes()));
        }
        if (request.vitals() != null) {
            visit.setVitalsJson(writeJson(request.vitals()));
        }
        return toModel(visitDao.save(visit), true);
    }

    @Override
    @Transactional
    public VisitModel completeVisit(String visitUuid, String email) {
        Visit visit = requireDoctorOwnedVisit(visitUuid, email);
        if (!FINISHABLE.contains(visit.getStatus())) {
            throw new CustomException(
                    "Finish treatment only while the visit is with the doctor (IN_PROGRESS). Current: "
                            + visit.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }
        if (visit.getAssessment() == null || visit.getAssessment().isBlank()) {
            throw new CustomException("Add an assessment / diagnosis before finishing treatment",
                    HttpStatus.BAD_REQUEST);
        }
        // Send to clinic Checkout — reception completes the visit afterward.
        applyStatusTransition(visit, VisitStatus.CHECKING_OUT);
        ensureHealthEvent(visit);
        if (visit.getClinicOwner() != null) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(visit.getClinicOwner());
        }
        return toModel(visitDao.save(visit), true);
    }

    @Override
    @Transactional
    public VisitModel returnToReception(String visitUuid, String email) {
        Visit visit = requireDoctorOwnedVisit(visitUuid, email);
        if (visit.getStatus() != VisitStatus.IN_PROGRESS && visit.getStatus() != VisitStatus.CHECKING_OUT) {
            throw new CustomException("Only in-progress visits can be sent back to reception",
                    HttpStatus.BAD_REQUEST);
        }
        applyStatusTransition(visit, VisitStatus.CHECKED_IN);
        return toModel(visitDao.save(visit), true);
    }

    @Override
    @Transactional
    public List<VisitModel> listParentPetVisits(String petUuid, String email) {
        User user = userDao.userByEmail(email);
        requireParentOwnsPet(user, petUuid);
        return visitDao.findCompletedByPetUuid(petUuid).stream()
                .map(v -> toModel(v, false))
                .toList();
    }

    @Override
    @Transactional
    public List<VisitModel> listMyParentVisits(String email) {
        User user = userDao.userByEmail(email);
        clinicOwnerUserLinkService.linkUserToClinicOwners(user);
        User managed = userDao.userByUuid(user.getUuid());
        List<String> petUuids = managed.getPets() == null ? List.of()
                : managed.getPets().stream().map(Pet::getUuid).filter(Objects::nonNull).distinct().toList();
        return visitDao.findForParentByPetUuids(petUuids).stream()
                .map(v -> toModel(v, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendedPatientModel> listMyAttendedPatients(String email) {
        DoctorProfile profile = requireDoctorProfile(email);
        Map<String, AttendedPatientModel> byPet = new HashMap<>();
        for (Visit visit : visitDao.findByDoctor(profile.getId())) {
            if (visit.getStatus() == VisitStatus.CANCELLED || visit.getStatus() == VisitStatus.NO_SHOW) {
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
            String assessment = visit.getAssessment();
            byPet.merge(pet.getUuid(),
                    new AttendedPatientModel(
                            pet.getUuid(),
                            pet.getName(),
                            pet.getType(),
                            pet.getBreed(),
                            owner.getUuid(),
                            clinicOwnerDisplayName(owner),
                            owner.getEmail(),
                            owner.getPhone(),
                            visit.getClinic() != null ? visit.getClinic().getUuid() : null,
                            visit.getClinic() != null ? visit.getClinic().getName() : null,
                            1,
                            when,
                            assessment),
                    (existing, added) -> new AttendedPatientModel(
                            existing.petUuid(),
                            existing.petName(),
                            existing.species(),
                            existing.breed(),
                            existing.ownerUuid(),
                            existing.ownerName(),
                            existing.ownerEmail(),
                            existing.ownerPhone(),
                            existing.clinicUuid(),
                            existing.clinicName(),
                            existing.visitCount() + 1,
                            laterVisit(existing.lastVisitAt(), added.lastVisitAt()),
                            preferText(added.lastAssessment(), existing.lastAssessment())));
        }
        return byPet.values().stream()
                .sorted(Comparator.comparing(AttendedPatientModel::lastVisitAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static LocalDateTime laterVisit(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    private static String preferText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private static String clinicOwnerDisplayName(ClinicPetOwner owner) {
        String last = owner.getLastName() == null ? "" : owner.getLastName().trim();
        return (owner.getFirstName() + (last.isEmpty() ? "" : " " + last)).trim();
    }

    private Pet resolvePetForWalkIn(Clinic clinic, WalkInCreateRequest request) {
        if (request.petUuid() != null && !request.petUuid().isBlank()) {
            return petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(request.petUuid(), clinic.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("pet", "uuid", request.petUuid()));
        }

        WalkInOwnerRequest ownerReq = request.owner();
        WalkInPetRequest petReq = request.newPet();
        if (ownerReq == null || petReq == null) {
            throw new CustomException("Provide petUuid or owner + newPet for walk-in", HttpStatus.BAD_REQUEST);
        }

        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(ownerReq.email());
        String phoneDigits = ClinicOwnerUserLinkService.normalizePhoneDigits(ownerReq.phone());
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
                    .firstName(ownerReq.firstName().trim())
                    .lastName(ownerReq.lastName() == null ? "" : ownerReq.lastName().trim())
                    .email(ownerEmail)
                    .phone(phoneDigits)
                    .address(blankToNull(ownerReq.address()))
                    .build();
            owner.setIsActive(true);
        } else {
            owner.setFirstName(ownerReq.firstName().trim());
            if (ownerReq.lastName() != null) {
                owner.setLastName(ownerReq.lastName().trim());
            }
            owner.setPhone(phoneDigits);
            if (ownerReq.address() != null && !ownerReq.address().isBlank()) {
                owner.setAddress(ownerReq.address().trim());
            }
        }
        owner = clinicPetOwnerRepository.save(owner);
        owner = clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);

        Pet pet = Pet.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(clinic)
                .clinicOwner(owner)
                .name(petReq.name().trim())
                .type(blankToNull(petReq.species()))
                .breed(blankToNull(petReq.breed()))
                .gender(blankToNull(petReq.gender()))
                .registeredAt(LocalDate.now())
                .isNeutered(false)
                .build();
        pet.setIsActive(true);
        pet = petsRepository.save(pet);
        if (owner.getLinkedUser() != null) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
        }
        return pet;
    }

    private void applyStatusTransition(Visit visit, VisitStatus target) {
        VisitStatus current = visit.getStatus();
        if (current == target) {
            return;
        }
        boolean allowed;
        if (target == VisitStatus.CHECKED_IN) {
            allowed = current == VisitStatus.WAITLIST || current == VisitStatus.IN_PROGRESS
                    || current == VisitStatus.CHECKING_OUT;
        } else if (target == VisitStatus.IN_PROGRESS) {
            allowed = current == VisitStatus.WAITLIST || current == VisitStatus.CHECKED_IN;
        } else if (target == VisitStatus.CHECKING_OUT) {
            allowed = current == VisitStatus.IN_PROGRESS;
        } else if (target == VisitStatus.COMPLETED) {
            allowed = current == VisitStatus.CHECKING_OUT;
        } else if (target == VisitStatus.CANCELLED || target == VisitStatus.NO_SHOW) {
            allowed = current == VisitStatus.WAITLIST || current == VisitStatus.CHECKED_IN;
        } else {
            allowed = false;
        }
        if (!allowed) {
            throw new CustomException("Invalid status transition " + current + " → " + target, HttpStatus.BAD_REQUEST);
        }
        visit.setStatus(target);
        LocalDateTime now = LocalDateTime.now();
        if (target == VisitStatus.CHECKED_IN && visit.getCheckedInAt() == null) {
            visit.setCheckedInAt(now);
        }
        if (target == VisitStatus.IN_PROGRESS) {
            if (visit.getCheckedInAt() == null) {
                visit.setCheckedInAt(now);
            }
            if (visit.getStartedAt() == null) {
                visit.setStartedAt(now);
            }
        }
        if (target == VisitStatus.COMPLETED) {
            visit.setCompletedAt(now);
        }
    }

    private void ensureHealthEvent(Visit visit) {
        if (visit.getHealthEventUuid() != null) {
            return;
        }
        String title = visit.getAssessment() != null && !visit.getAssessment().isBlank()
                ? visit.getAssessment()
                : (visit.getReasonForVisit() != null ? visit.getReasonForVisit() : "Clinic visit");
        HealthEvent event = HealthEvent.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(visit.getClinic())
                .pet(visit.getPet())
                .type(HealthEventType.VET_VISIT)
                .title(title)
                .description(buildPublicSummary(visit))
                .date(LocalDate.now())
                .isPast(true)
                .status(HealthEventStatus.COMPLETED)
                .build();
        event.setIsActive(true);
        event = healthEventDao.save(event);
        visit.setHealthEventUuid(event.getUuid());
    }

    private void notifyDoctorOfPatient(Visit visit, String action) {
        DoctorProfile doctor = visit.getDoctor();
        if (doctor == null || doctor.getUser() == null) {
            return;
        }
        String petName = visit.getPet() != null ? visit.getPet().getName() : "a pet";
        ClinicPetOwner owner = visit.getClinicOwner();
        String ownerLabel = owner == null ? "owner"
                : ((owner.getFirstName() == null ? "" : owner.getFirstName()) + " "
                        + (owner.getLastName() == null ? "" : owner.getLastName())).trim();
        String clinicName = visit.getClinic() != null ? visit.getClinic().getName() : "Clinic";
        String payload = String.format("%s %s %s (%s) — open My visits to start.", clinicName, action, petName,
                ownerLabel);
        User doctorUser = doctor.getUser();
        Pet pet = visit.getPet();

        Runnable persist = () -> {
            try {
                notificationLogRepository.save(NotificationLog.builder()
                        .user(doctorUser)
                        .pet(pet)
                        .type(NotificationType.CLINIC_VISIT_ASSIGNED)
                        .payload(payload)
                        .sentAt(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to notify doctor of patient assignment: {}", e.getMessage());
            }
        };

        // Persist after commit so a notification DB constraint failure cannot roll back the walk-in.
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

    private Visit requireDoctorOwnedVisit(String visitUuid, String email) {
        DoctorProfile profile = requireDoctorProfile(email);
        Visit visit = visitDao.findByUuid(visitUuid)
                .orElseThrow(() -> new ResourceNotFoundException("visit", "uuid", visitUuid));
        if (!clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(
                visit.getClinic().getId(), profile.getUser().getId())) {
            throw new AccessDeniedException("You are not a doctor at this clinic");
        }
        if (visit.getDoctor() != null && !visit.getDoctor().getId().equals(profile.getId())) {
            // Allow clinic doctors to pick up unassigned or reassigned — if assigned to other, deny chart
            throw new CustomException("This visit is assigned to another doctor", HttpStatus.FORBIDDEN);
        }
        if (visit.getDoctor() == null) {
            visit.setDoctor(profile);
        }
        return visit;
    }

    private DoctorProfile requireDoctorProfile(String email) {
        User user = userDao.userByEmail(email);
        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        return profile;
    }

    private DoctorProfile requireClinicDoctor(Clinic clinic, String doctorUuid) {
        return clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(clinic.getId(), doctorUuid)
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .map(a -> a.getDoctor())
                .orElseThrow(() -> new CustomException("Doctor is not active at this clinic", HttpStatus.BAD_REQUEST));
    }

    private Clinic access(String clinicUuid, String email) {
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        User user = userDao.userByEmail(email);
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(user.getId());
        boolean staff = clinicStaffDao.isActiveMember(clinic.getId(), user.getId());
        boolean doctor = clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(),
                user.getId());
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

    private void requireParentOwnsPet(User user, String petUuid) {
        boolean owns = user.getPets() != null
                && user.getPets().stream().anyMatch(p -> petUuid.equals(p.getUuid()));
        if (owns) {
            return;
        }
        Pet pet = petsRepository.findOptionalByUuid(petUuid).orElse(null);
        if (pet == null) {
            throw new AccessDeniedException("You do not have access to this pet");
        }
        ClinicPetOwner owner = pet.getClinicOwner();
        if (owner != null && owner.getLinkedUser() != null
                && owner.getLinkedUser().getId().equals(user.getId())) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
            return;
        }
        if (owner != null) {
            String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(owner.getEmail());
            String userEmail = ClinicOwnerUserLinkService.normalizeEmail(user.getEmail());
            if (ownerEmail != null && ownerEmail.equals(userEmail)) {
                clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
                return;
            }
            String ownerPhone = ClinicOwnerUserLinkService.normalizePhoneDigits(owner.getPhone());
            String userPhone = ClinicOwnerUserLinkService.normalizePhoneDigits(user.getPhoneNumber());
            if (ownerPhone != null && ownerPhone.equals(userPhone) && ownerPhone.matches("\\d{10}")) {
                clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
                return;
            }
        }
        throw new AccessDeniedException("You do not have access to this pet");
    }

    private VisitModel toModel(Visit visit, boolean includeInternal) {
        ClinicPetOwner owner = visit.getClinicOwner();
        String ownerName = owner == null ? null
                : ((owner.getFirstName() == null ? "" : owner.getFirstName()) + " "
                        + (owner.getLastName() == null ? "" : owner.getLastName())).trim();
        DoctorProfile doctor = visit.getDoctor();
        String doctorName = null;
        String doctorSpecialization = null;
        Double doctorExperienceYears = null;
        if (doctor != null) {
            if (doctor.getUser() != null) {
                doctorName = ((doctor.getUser().getFirstName() == null ? "" : doctor.getUser().getFirstName()) + " "
                        + (doctor.getUser().getLastName() == null ? "" : doctor.getUser().getLastName())).trim();
            }
            if (doctor.getSpecialization() != null) {
                doctorSpecialization = doctor.getSpecialization().getSpecialization();
            }
            doctorExperienceYears = doctor.getExperienceYears();
        }
        VisitChartModel chart = new VisitChartModel(
                visit.getExaminationNotes(),
                visit.getAssessment(),
                visit.getPlan(),
                visit.getNextVisitNotes(),
                readVitals(visit.getVitalsJson()),
                includeInternal ? visit.getInternalNotes() : null);
        return new VisitModel(
                visit.getUuid(),
                visit.getClinic().getUuid(),
                visit.getClinic().getName(),
                visit.getPet().getUuid(),
                visit.getPet().getName(),
                ownerName,
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getPhone(),
                doctor == null ? null : doctor.getUuid(),
                doctorName,
                doctorSpecialization,
                doctorExperienceYears,
                visit.getSource(),
                visit.getChannel(),
                visit.getStatus(),
                visit.getUrgency(),
                visit.getReasonForVisit(),
                visit.getCheckedInAt(),
                visit.getStartedAt(),
                visit.getCompletedAt(),
                visit.getCreatedAt(),
                chart,
                visit.getInvoiceUuid(),
                visit.getHealthEventUuid());
    }

    private String buildPublicSummary(Visit visit) {
        StringBuilder sb = new StringBuilder();
        if (visit.getReasonForVisit() != null) {
            sb.append("Reason: ").append(visit.getReasonForVisit());
        }
        if (visit.getExaminationNotes() != null) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("Report: ").append(visit.getExaminationNotes());
        }
        if (visit.getAssessment() != null) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("Diagnosis: ").append(visit.getAssessment());
        }
        if (visit.getPlan() != null) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("Plan: ").append(visit.getPlan());
        }
        return sb.length() == 0 ? "Visit completed" : sb.toString();
    }

    private Map<String, Object> readVitals(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(Map<String, Object> vitals) {
        try {
            return objectMapper.writeValueAsString(vitals);
        } catch (Exception e) {
            throw new CustomException("Invalid vitals payload", HttpStatus.BAD_REQUEST);
        }
    }

    private static int urgencyRank(VisitUrgency urgency) {
        return urgency == VisitUrgency.URGENT ? 1 : 0;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
