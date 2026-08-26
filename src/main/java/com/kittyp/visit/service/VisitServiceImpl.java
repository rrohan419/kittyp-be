package com.kittyp.visit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.entity.DoctorAvailability;
import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.booking.repository.DoctorAvailabilityRepository;
import com.kittyp.booking.service.JitsiMeetService;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorPatientEnrollment;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.entity.DoctorReview;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.doctor.repository.DoctorReviewRepository;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.health.entity.HealthEvent;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;
import com.kittyp.notification.entity.NotificationLog;
import com.kittyp.notification.enums.NotificationType;
import com.kittyp.notification.repository.NotificationLogRepository;
import com.kittyp.notification.service.OutboundMessageService;
import com.kittyp.notification.service.WhatsAppSenderCredentials;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.service.PetAccessGuard;
import com.kittyp.user.service.UserService;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.dto.VisitDtos.AttendedPatientModel;
import com.kittyp.visit.dto.VisitDtos.ParentBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingPatchRequest;
import com.kittyp.visit.dto.VisitDtos.VisitChartModel;
import com.kittyp.visit.dto.VisitDtos.VisitChartRequest;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.dto.VisitDtos.VisitPatchRequest;
import com.kittyp.visit.dto.VisitDtos.VisitRatingModel;
import com.kittyp.visit.dto.VisitDtos.VisitRatingRequest;
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
    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final int APPOINTMENT_MINUTES = 30;

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
    private final BookingRepository bookingRepository;
    private final DoctorReviewRepository doctorReviewRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final PetAccessGuard petAccessGuard;
    private final UserService userService;
    private final OutboundMessageService outboundMessageService;
    private final ObjectMapper objectMapper;
    private final ParentBookingEnrollmentService parentBookingEnrollmentService;
    private final ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    private final DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;
    private final JitsiMeetService jitsiMeetService;

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
            requirePracticeReady(doctor);
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
        parentBookingEnrollmentService.enrollAfterStaffCare(clinic, doctor, pet);
        if (doctor != null) {
            notifyDoctorOfPatient(visit, "assigned");
        }
        return toModel(visit, true);
    }

    @Override
    @Transactional
    public BookingModel createScheduledBooking(String clinicUuid, ScheduleBookingCreateRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);

        if (request.doctorUuid() == null || request.doctorUuid().isBlank()) {
            throw new CustomException("Doctor is required to schedule an appointment", HttpStatus.BAD_REQUEST);
        }
        if (request.slotStart() == null) {
            throw new CustomException("Appointment start time is required", HttpStatus.BAD_REQUEST);
        }

        DoctorProfile doctor = requireClinicDoctor(clinic, request.doctorUuid());
        requirePracticeReady(doctor);
        boolean selfBook = isSelfBooking(email, doctor);

        // Always snap to half-hour grid; self-book may still stack in the same window.
        LocalDateTime slotStart = snapToHalfHour(request.slotStart());
        LocalDateTime slotEnd = slotStart.plusMinutes(APPOINTMENT_MINUTES);
        requireWithinDoctorHours(doctor, slotStart);

        if (!selfBook) {
            List<Booking> conflicts = bookingRepository.findOverlappingForDoctor(
                    doctor.getId(), slotStart, slotEnd, ACTIVE_BOOKING_STATUSES);
            if (!conflicts.isEmpty()) {
                throw new CustomException("This time is already booked. Please try another slot.",
                        HttpStatus.CONFLICT);
            }
        }

        // Reuse walk-in pet/owner resolution (existing petUuid or new owner+pet).
        WalkInCreateRequest petRequest = new WalkInCreateRequest(
                request.petUuid(), request.owner(), request.newPet(), null, null, null);
        Pet pet = resolvePetForWalkIn(clinic, petRequest);
        ClinicPetOwner clinicOwner = pet.getClinicOwner();
        if (clinicOwner != null) {
            clinicOwner = clinicOwnerUserLinkService.linkOwnerIfUserExists(clinicOwner);
        }
        User ownerUser = resolvePlatformOwner(pet);

        BookingMode mode = request.mode() == null ? BookingMode.IN_PERSON : request.mode();
        String notes = blankToNull(request.notes());

        Booking booking = Booking.builder()
                .pet(pet)
                .owner(ownerUser)
                .doctor(doctor)
                .clinic(clinic)
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .timezone(clinic.getTimezone())
                .mode(mode)
                .status(BookingStatus.CONFIRMED)
                .notes(notes)
                .build();
        booking.setIsActive(true);
        booking = bookingRepository.save(booking);
        jitsiMeetService.ensureVideoRoom(booking);
        if (booking.getJitsiRoomId() != null) {
            booking = bookingRepository.save(booking);
        }

        parentBookingEnrollmentService.enrollAfterStaffCare(clinic, doctor, pet);
        notifyDoctorOfBooking(booking);
        return toBookingModel(booking);
    }

    /** Platform pet-parent for a clinic pet: linked CRM owner, then pets.user_uuid. */
    private User resolvePlatformOwner(Pet pet) {
        if (pet == null) {
            return null;
        }
        if (pet.getClinicOwner() != null) {
            ClinicPetOwner owner = clinicOwnerUserLinkService.linkOwnerIfUserExists(pet.getClinicOwner());
            if (owner != null && owner.getLinkedUser() != null) {
                return owner.getLinkedUser();
            }
        }
        Optional<User> byPet = userDao.findOptionalByPetUuid(pet.getUuid());
        if (byPet.isPresent()) {
            return byPet.get();
        }
        String parentUuid = pet.getParentUserUuid();
        if (parentUuid == null || parentUuid.isBlank()) {
            return null;
        }
        try {
            return userDao.userByUuid(parentUuid);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    @Override
    @Transactional
    public BookingModel updateScheduledBooking(String clinicUuid, String bookingUuid,
            ScheduleBookingPatchRequest request, String email) {
        Clinic clinic = access(clinicUuid, email);
        requireOperational(clinic);

        if (request == null) {
            throw new CustomException("No changes provided", HttpStatus.BAD_REQUEST);
        }

        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("booking", "uuid", bookingUuid));
        if (booking.getClinic() == null || !booking.getClinic().getId().equals(clinic.getId())) {
            throw new ResourceNotFoundException("booking", "uuid", bookingUuid);
        }

        BookingStatus current = booking.getStatus();
        if (current != BookingStatus.PENDING && current != BookingStatus.CONFIRMED) {
            throw new CustomException("This appointment can no longer be edited", HttpStatus.BAD_REQUEST);
        }

        boolean hasDoctor = request.doctorUuid() != null && !request.doctorUuid().isBlank();
        boolean hasSlot = request.slotStart() != null;
        boolean hasNotes = request.notes() != null;
        boolean hasStatus = request.status() != null;
        boolean hasMode = request.mode() != null;
        if (!hasDoctor && !hasSlot && !hasNotes && !hasStatus && !hasMode) {
            throw new CustomException("No changes provided", HttpStatus.BAD_REQUEST);
        }

        User actor = userDao.userByEmail(email);
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(actor.getId());
        boolean staff = clinicStaffDao.isActiveMember(clinic.getId(), actor.getId());
        DoctorProfile actorDoctor = null;
        if (!owner && !staff) {
            actorDoctor = doctorProfileDao.findByUserId(actor.getId());
            if (actorDoctor == null || booking.getDoctor() == null
                    || !actorDoctor.getId().equals(booking.getDoctor().getId())) {
                throw new AccessDeniedException("You can only edit your own appointments");
            }
            if (hasDoctor && !actorDoctor.getUuid().equals(request.doctorUuid().trim())) {
                throw new CustomException("You cannot reassign this appointment", HttpStatus.FORBIDDEN);
            }
        }

        if (hasStatus) {
            BookingStatus next = request.status();
            if (next != BookingStatus.CANCELLED && next != BookingStatus.NO_SHOW) {
                throw new CustomException("Clinic can only cancel or mark no-show from this screen",
                        HttpStatus.BAD_REQUEST);
            }
            booking.setStatus(next);
            return toBookingModel(bookingRepository.save(booking));
        }

        DoctorProfile doctor = booking.getDoctor();
        if (hasDoctor) {
            doctor = requireClinicDoctor(clinic, request.doctorUuid().trim());
            requirePracticeReady(doctor);
            booking.setDoctor(doctor);
        }
        if (doctor == null) {
            throw new CustomException("Doctor is required to schedule an appointment", HttpStatus.BAD_REQUEST);
        }

        boolean slotOrDoctorChanged = hasSlot || hasDoctor;
        if (hasSlot) {
            LocalDateTime slotStart = snapToHalfHour(request.slotStart());
            if (slotStart.isBefore(LocalDateTime.now().minusMinutes(5))) {
                throw new CustomException("Cannot book a slot in the past", HttpStatus.BAD_REQUEST);
            }
            booking.setSlotStart(slotStart);
            booking.setSlotEnd(slotStart.plusMinutes(APPOINTMENT_MINUTES));
        }

        if (slotOrDoctorChanged) {
            LocalDateTime slotStart = booking.getSlotStart();
            if (slotStart == null) {
                throw new CustomException("Appointment start time is required", HttpStatus.BAD_REQUEST);
            }
            LocalDateTime slotEnd = booking.getSlotEnd() != null
                    ? booking.getSlotEnd()
                    : slotStart.plusMinutes(APPOINTMENT_MINUTES);
            requireWithinDoctorHours(doctor, slotStart);
            List<Booking> conflicts = bookingRepository.findOverlappingForDoctor(
                    doctor.getId(), slotStart, slotEnd, ACTIVE_BOOKING_STATUSES);
            boolean clash = conflicts.stream().anyMatch(b -> !Objects.equals(b.getId(), booking.getId()));
            if (clash) {
                throw new CustomException("This time is already booked. Please try another slot.",
                        HttpStatus.CONFLICT);
            }
        }

        if (hasNotes) {
            booking.setNotes(blankToNull(request.notes()));
        }
        if (hasMode) {
            booking.setMode(request.mode());
        }
        jitsiMeetService.ensureVideoRoom(booking);
        return toBookingModel(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingModel createParentBooking(ParentBookingCreateRequest request, String email) {
        User owner = userDao.userByEmail(email);
        petAccessGuard.requireOwner(owner, request.petUuid());

        Clinic clinic = clinicDao.findByUuid(request.clinicUuid());
        if (clinic == null || Boolean.FALSE.equals(clinic.getIsActive())) {
            throw new ResourceNotFoundException("clinic", "uuid", request.clinicUuid());
        }
        requireOperational(clinic);

        DoctorProfile doctor = requireClinicDoctor(clinic, request.doctorUuid());
        requirePracticeReady(doctor);
        Pet pet = petsRepository.findOptionalByUuid(request.petUuid())
                .orElseThrow(() -> new ResourceNotFoundException("pet", "uuid", request.petUuid()));

        LocalDateTime slotStart = snapToHalfHour(request.slotStart());
        if (slotStart.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new CustomException("Cannot book a slot in the past", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime slotEnd = slotStart.plusMinutes(APPOINTMENT_MINUTES);
        requireWithinDoctorHours(doctor, slotStart);

        List<Booking> conflicts = bookingRepository.findOverlappingForDoctor(
                doctor.getId(), slotStart, slotEnd, ACTIVE_BOOKING_STATUSES);
        if (!conflicts.isEmpty()) {
            throw new CustomException("This time is already booked. Please try another slot.", HttpStatus.CONFLICT);
        }

        parentBookingEnrollmentService.enrollAfterParentBooking(clinic, doctor, pet, owner);

        BookingMode mode = request.mode() == null ? BookingMode.IN_PERSON : request.mode();
        if (ParentBookingEnrollmentService.isPersonalPractice(clinic, doctor)) {
            mode = BookingMode.VIDEO;
        }
        Booking booking = Booking.builder()
                .pet(pet)
                .owner(owner)
                .doctor(doctor)
                .clinic(clinic)
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .timezone(clinic.getTimezone() == null ? "Asia/Kolkata" : clinic.getTimezone())
                .mode(mode)
                .status(BookingStatus.CONFIRMED)
                .notes(blankToNull(request.notes()))
                .build();
        booking.setIsActive(true);
        booking = bookingRepository.save(booking);
        jitsiMeetService.ensureVideoRoom(booking);
        if (booking.getJitsiRoomId() != null) {
            booking = bookingRepository.save(booking);
        }

        notifyDoctorOfBooking(booking);
        return toBookingModel(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> listParentDoctorSlots(String clinicUuid, String doctorUuid, LocalDate date,
            String email) {
        // Authenticated caller only — no clinic staff check for discovery slots.
        userDao.userByEmail(email);
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null || Boolean.FALSE.equals(clinic.getIsActive())) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        requireOperational(clinic);
        DoctorProfile doctor = requireClinicDoctor(clinic, doctorUuid);
        LocalDate day = date == null ? LocalDate.now() : date;
        if (day.isBefore(LocalDate.now())) {
            return List.of();
        }

        int duration = APPOINTMENT_MINUTES;
        DoctorAvailability availability = doctorAvailabilityRepository.findByDoctor_Id(doctor.getId()).orElse(null);
        if (availability != null && availability.getSlotDurationMinutes() != null
                && availability.getSlotDurationMinutes() > 0) {
            duration = availability.getSlotDurationMinutes();
        }

        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(
                availability == null ? null : availability.getWeeklyScheduleJson(), day.getDayOfWeek());
        if (windows.isEmpty()) {
            return List.of();
        }

        LocalDateTime rangeFrom = day.atStartOfDay();
        LocalDateTime rangeTo = day.plusDays(1).atStartOfDay();
        List<Booking> busy = bookingRepository.findOverlappingForDoctor(
                doctor.getId(), rangeFrom, rangeTo, ACTIVE_BOOKING_STATUSES);

        List<LocalDateTime> free = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (LocalTime[] window : windows) {
            LocalDateTime cursor = day.atTime(window[0]);
            LocalDateTime windowEnd = day.atTime(window[1]);
            while (!cursor.plusMinutes(duration).isAfter(windowEnd)) {
                LocalDateTime slotEnd = cursor.plusMinutes(duration);
                if (!cursor.isBefore(now)) {
                    LocalDateTime start = cursor;
                    boolean conflict = busy.stream()
                            .anyMatch(b -> b.getSlotStart().isBefore(slotEnd) && b.getSlotEnd().isAfter(start));
                    if (!conflict) {
                        free.add(start);
                    }
                }
                cursor = cursor.plusMinutes(duration);
            }
        }
        return free;
    }

    private void requireWithinDoctorHours(DoctorProfile doctor, LocalDateTime slotStart) {
        DoctorAvailability availability = doctorAvailabilityRepository.findByDoctor_Id(doctor.getId()).orElse(null);
        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(
                availability == null ? null : availability.getWeeklyScheduleJson(), slotStart.getDayOfWeek());
        LocalTime start = slotStart.toLocalTime();
        LocalTime end = slotStart.plusMinutes(APPOINTMENT_MINUTES).toLocalTime();
        if (!DoctorHours.fitsWindow(windows, start, end)) {
            throw new CustomException("Doctor is not available at this time", HttpStatus.BAD_REQUEST);
        }
    }

    private void notifyDoctorOfBooking(Booking booking) {
        DoctorProfile doctor = booking.getDoctor();
        if (doctor == null || doctor.getUser() == null) {
            return;
        }
        User doctorUser = doctor.getUser();
        Pet pet = booking.getPet();
        String petName = pet != null ? pet.getName() : "a pet";
        String clinicName = booking.getClinic() != null ? booking.getClinic().getName() : "Clinic";
        String when = booking.getSlotStart() == null ? "soon" : booking.getSlotStart().toString();
        String title = "New appointment booked";
        String body = String.format("%s booked %s at %s (%s)", clinicName, petName, when,
                booking.getMode() == null ? "IN_PERSON" : booking.getMode().name());

        Runnable notify = () -> {
            try {
                notificationLogRepository.save(NotificationLog.builder()
                        .user(doctorUser)
                        .pet(pet)
                        .type(NotificationType.BOOKING_CREATED)
                        .payload(body)
                        .sentAt(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to log booking notification: {}", e.getMessage());
            }
            try {
                if (doctorUser.getEmail() != null) {
                    userService.sendPushNotification(doctorUser.getEmail(), title, body);
                }
            } catch (Exception e) {
                log.warn("Failed to push booking notification: {}", e.getMessage());
            }
            try {
                WhatsAppSenderCredentials sender = resolveBookingWhatsAppSender(booking);
                String phone = doctor.getPhoneNumber() != null ? doctor.getPhoneNumber() : doctorUser.getPhoneNumber();
                if (sender != null && sender.isConfigured() && phone != null && !phone.isBlank()) {
                    outboundMessageService.trySendAppointmentNotice(
                            sender, phone, List.of(petName, clinicName, when), doctorUser, pet);
                }
            } catch (Exception e) {
                log.warn("Failed to WhatsApp booking notification: {}", e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
        } else {
            notify.run();
        }
    }

    private WhatsAppSenderCredentials resolveBookingWhatsAppSender(Booking booking) {
        Clinic clinic = booking.getClinic();
        if (clinic != null) {
            WhatsAppSenderCredentials clinicSender = WhatsAppSenderCredentials.of(
                    clinic.getWhatsappToken(), clinic.getWhatsappPhoneNumberId());
            if (clinicSender.isConfigured()) {
                return clinicSender;
            }
        }
        DoctorProfile doctor = booking.getDoctor();
        if (doctor != null) {
            return WhatsAppSenderCredentials.of(doctor.getWhatsappToken(), doctor.getWhatsappPhoneNumberId());
        }
        return WhatsAppSenderCredentials.of(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingModel> listDoctorBusySlots(String clinicUuid, String doctorUuid, LocalDateTime from,
            LocalDateTime to, String email) {
        Clinic clinic = access(clinicUuid, email);
        DoctorProfile doctor = requireClinicDoctor(clinic, doctorUuid);
        LocalDateTime rangeFrom = from == null ? LocalDate.now().atStartOfDay() : from;
        LocalDateTime rangeTo = to == null ? rangeFrom.plusDays(1) : to;
        if (!rangeTo.isAfter(rangeFrom)) {
            throw new CustomException("Busy range end must be after start", HttpStatus.BAD_REQUEST);
        }
        return bookingRepository
                .findOverlappingForDoctor(doctor.getId(), rangeFrom, rangeTo, ACTIVE_BOOKING_STATUSES)
                .stream()
                .map(this::toBookingModel)
                .toList();
    }

    @Override
    @Transactional
    public VisitModel startTreatmentFromBooking(String bookingUuid, String email) {
        DoctorProfile profile = requireDoctorProfile(email);
        requirePracticeReady(profile);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("booking", "uuid", bookingUuid));
        if (booking.getDoctor() == null || !booking.getDoctor().getId().equals(profile.getId())) {
            throw new AccessDeniedException("This booking is not assigned to you");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new CustomException("Cannot start treatment for a cancelled booking", HttpStatus.BAD_REQUEST);
        }
        if (booking.getClinic() == null || booking.getPet() == null) {
            throw new CustomException("Booking is missing clinic or pet", HttpStatus.BAD_REQUEST);
        }
        requireOperational(booking.getClinic());

        // Idempotent: booking already converted — return today's open visit for this pet if any.
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            LocalDateTime from = LocalDate.now().atStartOfDay();
            LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);
            Optional<Visit> open = visitDao.findByDoctorAndDay(profile.getId(), from, to).stream()
                    .filter(v -> v.getPet() != null && v.getPet().getId().equals(booking.getPet().getId()))
                    .filter(v -> v.getStatus() == VisitStatus.IN_PROGRESS
                            || v.getStatus() == VisitStatus.CHECKING_OUT
                            || v.getStatus() == VisitStatus.CHECKED_IN)
                    .findFirst();
            if (open.isPresent()) {
                return toModel(open.get(), true);
            }
            throw new CustomException("Treatment already started for this booking", HttpStatus.BAD_REQUEST);
        }

        ClinicPetOwner owner = booking.getPet().getClinicOwner();
        if (owner != null) {
            owner = clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
        }
        User platformOwner = resolvePlatformOwner(booking.getPet());
        if (booking.getOwner() == null && platformOwner != null) {
            booking.setOwner(platformOwner);
            bookingRepository.save(booking);
        }

        LocalDateTime now = LocalDateTime.now();
        Visit visit = Visit.builder()
                .uuid(UUID.randomUUID().toString())
                .clinic(booking.getClinic())
                .pet(booking.getPet())
                .clinicOwner(owner)
                .doctor(profile)
                .source(VisitSource.SCHEDULED)
                .channel(booking.getMode() == null ? BookingMode.IN_PERSON : booking.getMode())
                .status(VisitStatus.IN_PROGRESS)
                .urgency(VisitUrgency.ROUTINE)
                .reasonForVisit(blankToNull(booking.getNotes()))
                .checkedInAt(now)
                .startedAt(now)
                .build();
        visit.setIsActive(true);
        visit = visitDao.save(visit);

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        if (platformOwner != null) {
            parentBookingEnrollmentService.enrollAfterParentBooking(
                    booking.getClinic(), profile, booking.getPet(), platformOwner);
        } else {
            parentBookingEnrollmentService.enrollAfterStaffCare(booking.getClinic(), profile, booking.getPet());
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
                requirePracticeReady(next);
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
            requireAssignedDoctor(visit, request.status());
            if (request.status() == VisitStatus.IN_PROGRESS && visit.getDoctor() != null) {
                requirePracticeReady(visit.getDoctor());
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
    public List<VisitModel> listMyDoctorVisits(LocalDate date, String clinicUuid, String email) {
        LocalDate day = date == null ? LocalDate.now() : date;
        return listMyDoctorVisitsRange(day, day, clinicUuid, email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitModel> listMyDoctorVisitsRange(LocalDate from, LocalDate to, String clinicUuid, String email) {
        DoctorProfile profile = requireDoctorProfile(email);
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        LocalDateTime rangeFrom = start.atStartOfDay();
        LocalDateTime rangeTo = end.atTime(LocalTime.MAX);
        return visitDao.findByDoctorAndDay(profile.getId(), rangeFrom, rangeTo).stream()
                .filter(v -> clinicUuid == null || clinicUuid.isBlank()
                        || (v.getClinic() != null && clinicUuid.equals(v.getClinic().getUuid())))
                .map(v -> toModel(v, true))
                .toList();
    }

    @Override
    @Transactional
    public VisitModel startVisit(String visitUuid, String email) {
        Visit visit = requireDoctorOwnedVisit(visitUuid, email);
        requirePracticeReady(requireDoctorProfile(email));
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
        if (visit.getStatus() == VisitStatus.CANCELLED || visit.getStatus() == VisitStatus.NO_SHOW) {
            throw new CustomException("Cannot edit chart for a closed visit", HttpStatus.BAD_REQUEST);
        }
        if (chartLocked(visit)) {
            throw new CustomException(
                    "Prescription can no longer be edited. More than one hour has passed since the visit was finished.",
                    HttpStatus.BAD_REQUEST);
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

    /** CHECKING_OUT / COMPLETED: chart (prescription) stays editable for 1 hour after finish. */
    private boolean chartLocked(Visit visit) {
        VisitStatus status = visit.getStatus();
        if (status == VisitStatus.WAITLIST || status == VisitStatus.CHECKED_IN || status == VisitStatus.IN_PROGRESS) {
            return false;
        }
        if (status != VisitStatus.CHECKING_OUT && status != VisitStatus.COMPLETED) {
            return true;
        }
        LocalDateTime doneAt = visit.getCheckingOutAt() != null ? visit.getCheckingOutAt() : visit.getCompletedAt();
        if (doneAt == null) {
            return true;
        }
        return doneAt.isBefore(LocalDateTime.now().minusHours(1));
    }

    @Override
    @Transactional
    public VisitModel completeVisit(String visitUuid, String email) {
        Visit visit = requireDoctorOwnedVisit(visitUuid, email);
        // Idempotent: already finished treatment / closed.
        if (visit.getStatus() == VisitStatus.CHECKING_OUT || visit.getStatus() == VisitStatus.COMPLETED) {
            return toModel(visit, true);
        }
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
        List<String> petUuids = parentVisiblePetUuids(managed);
        return visitDao.findForParentUser(managed.getId(), managed.getUuid(), managed.getEmail(), petUuids).stream()
                .map(v -> toModel(v, false))
                .toList();
    }

    @Override
    @Transactional
    public List<BookingModel> listMyParentBookings(String email) {
        User user = userDao.userByEmail(email);
        clinicOwnerUserLinkService.linkUserToClinicOwners(user);
        User managed = userDao.userByUuid(user.getUuid());
        List<String> petUuids = parentVisiblePetUuids(managed);
        String userEmail = managed.getEmail() == null || managed.getEmail().isBlank()
                ? "__none__"
                : managed.getEmail().trim();
        List<Booking> bookings = petUuids.isEmpty()
                ? bookingRepository.findForParentUserWithoutPets(managed.getId(), managed.getUuid(), userEmail)
                : bookingRepository.findForParentUser(managed.getId(), managed.getUuid(), userEmail, petUuids);
        for (Booking booking : bookings) {
            if (booking.getOwner() == null && booking.getPet() != null) {
                User owner = resolvePlatformOwner(booking.getPet());
                if (owner != null) {
                    booking.setOwner(owner);
                    bookingRepository.save(booking);
                }
            }
        }
        return bookings.stream().map(this::toBookingModel).toList();
    }

    @Override
    @Transactional
    public VisitRatingModel rateVisit(String visitUuid, VisitRatingRequest request, String email) {
        if (request == null || request.stars() == null || request.stars() < 1 || request.stars() > 5) {
            throw new CustomException("Stars must be between 1 and 5", HttpStatus.BAD_REQUEST);
        }
        User user = userDao.userByEmail(email);
        Visit visit = visitDao.findByUuid(visitUuid)
                .orElseThrow(() -> new ResourceNotFoundException("visit", "uuid", visitUuid));
        requireParentOwnsPet(user, visit.getPet().getUuid());
        if (visit.getStatus() != VisitStatus.COMPLETED) {
            throw new CustomException("Only completed visits can be rated", HttpStatus.BAD_REQUEST);
        }
        DoctorProfile doctor = visit.getDoctor();
        if (doctor == null) {
            throw new CustomException("This visit has no assigned doctor to rate", HttpStatus.BAD_REQUEST);
        }
        if (doctorReviewRepository.existsByVisit_Uuid(visitUuid)) {
            throw new CustomException("This visit was already rated", HttpStatus.CONFLICT);
        }
        String comment = request.comment() == null ? null : request.comment().trim();
        if (comment != null && comment.isEmpty()) {
            comment = null;
        }
        if (comment != null && comment.length() > 1000) {
            throw new CustomException("Comment must be at most 1000 characters", HttpStatus.BAD_REQUEST);
        }
        DoctorReview review = DoctorReview.builder()
                .uuid(UUID.randomUUID().toString())
                .doctor(doctor)
                .clinic(visit.getClinic())
                .visit(visit)
                .reviewer(user)
                .stars(request.stars())
                .comment(comment)
                .build();
        review.setIsActive(true);
        try {
            doctorReviewRepository.save(review);
        } catch (DataIntegrityViolationException ex) {
            throw new CustomException("This visit was already rated", HttpStatus.CONFLICT);
        }
        recomputeDoctorRating(doctor);
        return new VisitRatingModel(
                visit.getUuid(),
                doctor.getUuid(),
                request.stars(),
                ratingLabel(request.stars().doubleValue()),
                doctor.getRating(),
                doctor.getReviewsCount());
    }

    private void recomputeDoctorRating(DoctorProfile doctor) {
        List<DoctorReview> reviews = doctorReviewRepository.findByDoctor_IdAndIsActiveTrue(doctor.getId());
        if (reviews.isEmpty()) {
            doctor.setRating(null);
            doctor.setReviewsCount(0);
        } else {
            double avg = reviews.stream().mapToInt(DoctorReview::getStars).average().orElse(0);
            doctor.setRating(Math.round(avg * 10.0) / 10.0);
            doctor.setReviewsCount(reviews.size());
        }
        doctorProfileDao.save(doctor);
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

    @Override
    @Transactional(readOnly = true)
    public List<AttendedPatientModel> listMyAttendedPatients(String email, String clinicUuid) {
        DoctorProfile profile = requireDoctorProfile(email);
        Map<String, AttendedPatientModel> byPet = new HashMap<>();
        // Only visits where this doctor actually treated the pet (not waitlist assignment alone).
        Set<VisitStatus> seenStatuses = EnumSet.of(
                VisitStatus.IN_PROGRESS, VisitStatus.CHECKING_OUT, VisitStatus.COMPLETED);
        for (Visit visit : visitDao.findByDoctor(profile.getId())) {
            if (!seenStatuses.contains(visit.getStatus())) {
                continue;
            }
            if (clinicUuid != null && !clinicUuid.isBlank()
                    && (visit.getClinic() == null || !clinicUuid.equals(visit.getClinic().getUuid()))) {
                continue;
            }
            absorbAttendedVisit(visit, byPet);
        }
        Clinic scoped = clinicUuid == null || clinicUuid.isBlank() ? null : clinicDao.findByUuid(clinicUuid);
        boolean personalScope = scoped != null && ParentBookingEnrollmentService.isPersonalPractice(scoped, profile);
        if (scoped != null && !personalScope) {
            for (Visit visit : visitDao.findByClinicAndStatuses(scoped.getId(), seenStatuses)) {
                if (visit.getDoctor() != null && visit.getDoctor().getId().equals(profile.getId())) {
                    continue;
                }
                absorbAttendedVisit(visit, byPet);
            }
        }
        if (personalScope || scoped == null) {
            for (DoctorPatientEnrollment enrollment : doctorPatientEnrollmentRepository
                    .findByDoctor_IdAndIsActiveTrue(profile.getId())) {
                absorbEnrollment(enrollment, byPet, personalScope ? scoped : null);
            }
        }
        return byPet.values().stream()
                .sorted(Comparator.comparing(AttendedPatientModel::lastVisitAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void absorbAttendedVisit(Visit visit, Map<String, AttendedPatientModel> byPet) {
        Pet pet = visit.getPet();
        if (pet == null) {
            return;
        }
        ClinicPetOwner clinicOwner = visit.getClinicOwner() != null ? visit.getClinicOwner() : pet.getClinicOwner();
        User platformOwner = platformOwnerFor(pet, visit.getDoctor(), clinicOwner);
        if (clinicOwner == null && platformOwner == null) {
            return;
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
                        clinicOwner != null ? clinicOwner.getUuid() : platformOwner.getUuid(),
                        clinicOwner != null ? clinicOwnerDisplayName(clinicOwner) : userDisplayName(platformOwner),
                        clinicOwner != null ? clinicOwner.getEmail() : platformOwner.getEmail(),
                        clinicOwner != null ? clinicOwner.getPhone() : platformOwner.getPhoneNumber(),
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
                        laterAssessment(existing, added)));
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

    private static String laterAssessment(AttendedPatientModel existing, AttendedPatientModel added) {
        boolean addedIsLater = existing.lastVisitAt() == null
                || (added.lastVisitAt() != null && !added.lastVisitAt().isBefore(existing.lastVisitAt()));
        return addedIsLater
                ? preferText(added.lastAssessment(), existing.lastAssessment())
                : preferText(existing.lastAssessment(), added.lastAssessment());
    }

    private static String preferText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private void absorbEnrollment(DoctorPatientEnrollment enrollment, Map<String, AttendedPatientModel> byPet,
            Clinic personalClinic) {
        Pet pet = enrollment.getPet();
        User owner = enrollment.getOwnerUser();
        if (pet == null || owner == null || byPet.containsKey(pet.getUuid())) {
            return;
        }
        LocalDateTime when = enrollment.getUpdatedAt() != null ? enrollment.getUpdatedAt() : enrollment.getCreatedAt();
        byPet.put(pet.getUuid(), new AttendedPatientModel(
                pet.getUuid(),
                pet.getName(),
                pet.getType(),
                pet.getBreed(),
                owner.getUuid(),
                userDisplayName(owner),
                owner.getEmail(),
                owner.getPhoneNumber(),
                personalClinic != null ? personalClinic.getUuid() : null,
                personalClinic != null ? personalClinic.getName() : null,
                0,
                when,
                null));
    }

    private User platformOwnerFor(Pet pet, DoctorProfile doctor, ClinicPetOwner clinicOwner) {
        if (clinicOwner != null && clinicOwner.getLinkedUser() != null) {
            return clinicOwner.getLinkedUser();
        }
        if (doctor != null && pet.getId() != null) {
            Optional<DoctorPatientEnrollment> enrollment = doctorPatientEnrollmentRepository
                    .findByDoctor_IdAndPet_IdAndIsActiveTrue(doctor.getId(), pet.getId());
            if (enrollment.isPresent() && enrollment.get().getOwnerUser() != null) {
                return enrollment.get().getOwnerUser();
            }
        }
        if (pet.getUuid() == null) {
            return null;
        }
        return userDao.findOptionalByPetUuid(pet.getUuid()).orElse(null);
    }

    private static String userDisplayName(User user) {
        if (user == null) {
            return "Owner";
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isBlank() ? (user.getEmail() == null ? "Owner" : user.getEmail()) : name;
    }

    private static String clinicOwnerDisplayName(ClinicPetOwner owner) {
        String last = owner.getLastName() == null ? "" : owner.getLastName().trim();
        return (owner.getFirstName() + (last.isEmpty() ? "" : " " + last)).trim();
    }

    private Pet resolvePetForWalkIn(Clinic clinic, WalkInCreateRequest request) {
        if (request.petUuid() != null && !request.petUuid().isBlank()) {
            Optional<Pet> byClinic = petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(request.petUuid(),
                    clinic.getId());
            if (byClinic.isPresent()) {
                return byClinic.get();
            }
            Optional<Pet> enrolled = clinicPetEnrollmentRepository
                    .findByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), request.petUuid())
                    .map(ClinicPetEnrollment::getPet)
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()));
            if (enrolled.isPresent()) {
                return enrolled.get();
            }
            Pet byPublicId = petsRepository.findByUuidIgnoreCase(request.petUuid().trim()).orElse(null);
            if (byPublicId != null && Boolean.TRUE.equals(byPublicId.getIsActive())) {
                if (byPublicId.getClinic() != null && byPublicId.getClinic().getId().equals(clinic.getId())) {
                    return byPublicId;
                }
                DoctorProfile practiceDoctor = clinic.getOwner() == null
                        ? null
                        : doctorProfileDao.findByUserId(clinic.getOwner().getId());
                if (practiceDoctor != null
                        && ParentBookingEnrollmentService.isPersonalPractice(clinic, practiceDoctor)
                        && doctorPatientEnrollmentRepository.existsByDoctor_IdAndPet_UuidAndIsActiveTrue(
                                practiceDoctor.getId(), byPublicId.getUuid())) {
                    return byPublicId;
                }
            }
            throw new ResourceNotFoundException("pet", "uuid", request.petUuid());
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
                .clinic(clinic)
                .clinicOwner(owner)
                .name(petReq.name().trim())
                .type(blankToNull(petReq.species()))
                .breed(blankToNull(petReq.breed()))
                .gender(blankToNull(petReq.gender()))
                .profilePicture(blankToNull(petReq.photoUrl()))
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
        Set<VisitStatus> flow = EnumSet.of(
                VisitStatus.WAITLIST, VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS, VisitStatus.CHECKING_OUT);
        Set<VisitStatus> earlierThanCheckout = EnumSet.of(
                VisitStatus.WAITLIST, VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS);
        if (flow.contains(target) && flow.contains(current)) {
            if (current == VisitStatus.CHECKING_OUT && earlierThanCheckout.contains(target)) {
                // Leave Checkout only within 30 minutes of entering it.
                LocalDateTime checkingOutAt = visit.getCheckingOutAt();
                allowed = checkingOutAt != null
                        && !checkingOutAt.isBefore(LocalDateTime.now().minusMinutes(30));
            } else {
                allowed = true;
            }
        } else if (flow.contains(target) && current == VisitStatus.COMPLETED) {
            // Reopen within 30 minutes of completion (e.g. dragged back from done).
            LocalDateTime completedAt = visit.getCompletedAt();
            allowed = completedAt != null
                    && !completedAt.isBefore(LocalDateTime.now().minusMinutes(30));
            if (allowed && doctorReviewRepository.existsByVisit_Uuid(visit.getUuid())) {
                throw new CustomException(
                        "Cannot reopen a visit that already has a parent rating",
                        HttpStatus.BAD_REQUEST);
            }
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
        if (target == VisitStatus.CHECKING_OUT) {
            visit.setCheckingOutAt(now);
        }
        if (target == VisitStatus.COMPLETED) {
            visit.setCompletedAt(now);
        } else if (current == VisitStatus.COMPLETED && flow.contains(target)) {
            visit.setCompletedAt(null);
            clearHealthEventForReopen(visit);
        } else if (current == VisitStatus.CHECKING_OUT && earlierThanCheckout.contains(target)) {
            visit.setCheckingOutAt(null);
            // Doctor finish already wrote a health event; undo if pulled back into active flow.
            clearHealthEventForReopen(visit);
        }
    }

    /** Waitlist / checked-in visits cannot enter With doctor, Checkout, or Completed without a doctor. */
    static void requireAssignedDoctor(Visit visit, VisitStatus target) {
        if (visit.getDoctor() != null) {
            return;
        }
        if (target == VisitStatus.IN_PROGRESS) {
            throw new CustomException("Assign a doctor before moving to With doctor", HttpStatus.BAD_REQUEST);
        }
        if (target == VisitStatus.CHECKING_OUT) {
            throw new CustomException("Assign a doctor before moving to Checkout", HttpStatus.BAD_REQUEST);
        }
        if (target == VisitStatus.COMPLETED) {
            throw new CustomException("Assign a doctor before completing the visit", HttpStatus.BAD_REQUEST);
        }
    }

    private void clearHealthEventForReopen(Visit visit) {
        String eventUuid = visit.getHealthEventUuid();
        if (eventUuid == null) {
            return;
        }
        healthEventDao.findByUuid(eventUuid).ifPresent(event -> {
            event.setIsActive(false);
            healthEventDao.save(event);
        });
        visit.setHealthEventUuid(null);
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

    private BookingModel toBookingModel(Booking booking) {
        String ownerName = null;
        if (booking.getOwner() != null) {
            ownerName = ((booking.getOwner().getFirstName() == null ? "" : booking.getOwner().getFirstName()) + " "
                    + (booking.getOwner().getLastName() == null ? "" : booking.getOwner().getLastName())).trim();
        } else if (booking.getPet() != null && booking.getPet().getClinicOwner() != null) {
            ownerName = clinicOwnerDisplayName(booking.getPet().getClinicOwner());
        }
        String petName = booking.getPet() == null ? "Pet" : booking.getPet().getName();
        String petUuid = booking.getPet() == null ? null : booking.getPet().getUuid();
        String doctorName = null;
        String doctorSpecialization = null;
        String doctorPhotoUrl = null;
        if (booking.getDoctor() != null) {
            doctorPhotoUrl = booking.getDoctor().getPhotoUrl();
            if (booking.getDoctor().getSpecialization() != null) {
                doctorSpecialization = booking.getDoctor().getSpecialization().name();
            }
            if (booking.getDoctor().getUser() != null) {
                doctorName = ((booking.getDoctor().getUser().getFirstName() == null ? ""
                        : booking.getDoctor().getUser().getFirstName())
                        + " "
                        + (booking.getDoctor().getUser().getLastName() == null ? ""
                                : booking.getDoctor().getUser().getLastName())).trim();
                if (doctorName.isBlank()) {
                    doctorName = null;
                }
            }
        }
        return new BookingModel(
                booking.getUuid(),
                petUuid,
                petName,
                ownerName,
                booking.getDoctor() == null ? null : booking.getDoctor().getUuid(),
                booking.getSlotStart(),
                booking.getSlotEnd(),
                booking.getTimezone(),
                booking.getStatus(),
                booking.getMode() == null ? null : booking.getMode().name(),
                booking.getNotes(),
                booking.getClinic() == null ? null : booking.getClinic().getUuid(),
                booking.getClinic() == null ? null : booking.getClinic().getName(),
                doctorName,
                doctorSpecialization,
                doctorPhotoUrl,
                booking.getPet() == null ? null : booking.getPet().getType(),
                booking.getVideoJoinUrl());
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

    private boolean isSelfBooking(String email, DoctorProfile assignedDoctor) {
        if (assignedDoctor == null) {
            return false;
        }
        try {
            User user = userDao.userByEmail(email);
            DoctorProfile actor = doctorProfileDao.findByUserId(user.getId());
            return actor != null && actor.getId().equals(assignedDoctor.getId());
        } catch (Exception e) {
            return false;
        }
    }

    /** Minutes 0 stay :00; 1–30 → :30; >30 → next hour :00. */
    private LocalDateTime snapToHalfHour(LocalDateTime t) {
        LocalDateTime base = t.withSecond(0).withNano(0);
        int minutes = base.getMinute();
        if (minutes == 0) {
            return base.withMinute(0);
        }
        if (minutes <= 30) {
            return base.withMinute(30);
        }
        return base.plusHours(1).withMinute(0);
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
        if (pet.getParentUserUuid() != null && pet.getParentUserUuid().equalsIgnoreCase(user.getUuid())) {
            return;
        }
        ClinicPetOwner owner = pet.getClinicOwner();
        if (owner != null && owner.getLinkedUser() != null
                && owner.getLinkedUser().getId().equals(user.getId())) {
            clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
            return;
        }
        // Already linked to a different platform user — do not claim via email/phone.
        if (owner != null && owner.getLinkedUser() != null
                && !owner.getLinkedUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this pet");
        }
        if (owner != null) {
            String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(owner.getEmail());
            String userEmail = ClinicOwnerUserLinkService.normalizeEmail(user.getEmail());
            if (ownerEmail != null && ownerEmail.equals(userEmail)) {
                clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
                return;
            }
            // Phone claim only when the digits uniquely match this user (same rule as link service).
            String ownerPhone = ClinicOwnerUserLinkService.normalizePhoneDigits(owner.getPhone());
            String userPhone = ClinicOwnerUserLinkService.normalizePhoneDigits(user.getPhoneNumber());
            if (ownerPhone != null && ownerPhone.equals(userPhone) && ownerPhone.matches("\\d{10}")
                    && clinicOwnerUserLinkService.isUniquePhoneMatch(user, ownerPhone)) {
                clinicOwnerUserLinkService.linkOwnerIfUserExists(owner);
                return;
            }
        }
        throw new AccessDeniedException("You do not have access to this pet");
    }

    private List<String> parentVisiblePetUuids(User managed) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (managed.getPets() != null) {
            for (Pet pet : managed.getPets()) {
                if (pet != null && pet.getUuid() != null) {
                    ids.add(pet.getUuid());
                }
            }
        }
        for (Pet pet : petsRepository.findByParentUserUuid(managed.getUuid())) {
            if (pet != null && pet.getUuid() != null && !Boolean.FALSE.equals(pet.getIsActive())) {
                ids.add(pet.getUuid());
            }
        }
        return new ArrayList<>(ids);
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
        Integer parentRating = doctorReviewRepository.findByVisit_Uuid(visit.getUuid())
                .map(DoctorReview::getStars)
                .orElse(null);
        return new VisitModel(
                visit.getUuid(),
                visit.getClinic().getUuid(),
                visit.getClinic().getName(),
                visit.getPet().getUuid(),
                visit.getPet().getName(),
                visit.getPet().getType(),
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
                visit.getCheckingOutAt(),
                visit.getCreatedAt(),
                chart,
                visit.getInvoiceUuid(),
                visit.getHealthEventUuid(),
                parentRating);
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
