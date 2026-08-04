package com.kittyp.clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventRequest;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.clinic.dto.ClinicDtos.VaccineScheduleModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.health.entity.HealthEvent;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.vaccine.dao.PetVaccineScheduleDao;
import com.kittyp.vaccine.entity.PetVaccineSchedule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicServiceImpl implements ClinicService {

    private final ClinicDao clinicDao;
    private final ClinicStaffDao clinicStaffDao;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final BookingDao bookingDao;
    private final HealthEventDao healthEventDao;
    private final PetVaccineScheduleDao petVaccineScheduleDao;
    private final NotificationLogRepository notificationLogRepository;
    private final UserDao userDao;
    private final PetDao petDao;

    @Override
    public List<ClinicModel> mine(String email) {
        User user = userDao.userByEmail(email);
        Map<Long, Clinic> clinics = new HashMap<>();
        clinicDao.findAllByOwnerUserId(user.getId()).forEach(clinic -> clinics.put(clinic.getId(), clinic));
        clinicStaffDao.findActiveByUserId(user.getId()).forEach(staff -> clinics.put(staff.getClinic().getId(), staff.getClinic()));
        clinicDoctorRepository.findAll().stream()
                .filter(affiliation -> Boolean.TRUE.equals(affiliation.getIsActive())
                        && affiliation.getDoctor().getUser().getId().equals(user.getId()))
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
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    public ClinicModel get(String clinicUuid, String email) {
        return clinicModel(access(clinicUuid, email));
    }

    @Override
    @Transactional
    public ClinicModel update(String clinicUuid, ClinicRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        clinic.setName(request.name());
        clinic.setAddress(request.address());
        clinic.setPhone(request.phone());
        clinic.setTimezone(request.timezone());
        clinic.setOperatingHours(request.operatingHours());
        return clinicModel(clinicDao.saveClinic(clinic));
    }

    @Override
    public List<DoctorModel> doctors(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        return clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).stream().map(this::doctorModel).toList();
    }

    @Override
    public List<PatientModel> patients(String clinicUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        return patientMap(clinic).values().stream().sorted(Comparator.comparing(PatientModel::lastVisit,
                Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    @Override
    public PatientDetailModel patientDetail(String clinicUuid, String petUuid, String email) {
        Clinic clinic = access(clinicUuid, email);
        PatientModel patient = requirePatient(clinic, petUuid);
        return new PatientDetailModel(patient, healthEventsFor(clinic.getId(), petUuid),
                petVaccineScheduleDao.findByPetUuid(petUuid).stream().map(this::vaccineModel).toList());
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
            return new PatientModel(uuid, pet.getName(), fullName(owner), owner.getEmail(), lastVisits.get(uuid));
        }));
    }

    private PatientModel requirePatient(Clinic clinic, String petUuid) {
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
                affiliation.getRole(), affiliation.getIsActive());
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
