package com.kittyp.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.service.OutboundMessageService;
import com.kittyp.notification.service.WhatsAppService;
import com.kittyp.notification.service.AutomationMessageBridge;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.PetReminderDtos.PetReminderModel;
import com.kittyp.user.dto.PetReminderDtos.PetReminderRequest;
import com.kittyp.user.dto.PetReminderDtos.PetReminderUpdateRequest;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.PetReminder;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.PetReminderType;
import com.kittyp.user.repository.PetReminderRepository;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.vaccine.entity.PetVaccineSchedule;
import com.kittyp.vaccine.repository.PetVaccineScheduleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetReminderService {

    private static final DateTimeFormatter EMAIL_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter EMAIL_TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private final ZeptoMailService zeptoMailService;
    private final PetReminderRepository petReminderRepository;
    private final PetVaccineScheduleRepository petVaccineScheduleRepository;
    private final PetsRepository petsRepository;
    private final ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    private final UserDao userDao;
    private final PetAccessGuard petAccessGuard;
    private final UserService userService;
    private final OutboundMessageService outboundMessageService;
    private final WhatsAppService whatsAppService;
    private final AutomationMessageBridge automationMessageBridge;

    @Transactional(readOnly = true)
    public List<PetReminderModel> listMine(String email) {
        User user = userDao.userByEmail(email);
        return petReminderRepository.findByUser_IdAndIsActiveTrueOrderByDueAtAsc(user.getId()).stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional
    public PetReminderModel create(PetReminderRequest request, String email) {
        User user = userDao.userByEmail(email);
        petAccessGuard.requireOwner(user, request.petUuid());
        Pet pet = petsRepository.findOptionalByUuid(request.petUuid())
                .orElseThrow(() -> new ResourceNotFoundException("pet", "uuid", request.petUuid()));
        if (request.dueAt() == null) {
            throw new CustomException("dueAt is required", HttpStatus.BAD_REQUEST);
        }
        PetReminder reminder = PetReminder.builder()
                .uuid(UUID.randomUUID().toString())
                .user(user)
                .pet(pet)
                .type(request.type())
                .dueAt(request.dueAt())
                .note(blankToNull(request.note()))
                .pushEnabled(request.pushEnabled() == null || request.pushEnabled())
                .whatsappEnabled(request.whatsappEnabled() == null || request.whatsappEnabled())
                .build();
        reminder.setIsActive(true);
        reminder=petReminderRepository.save(reminder);
        scheduleAutomation(reminder);
        return toModel(reminder);
    }

    @Transactional
    public PetReminderModel update(String reminderUuid, PetReminderUpdateRequest request, String email) {
        User user = userDao.userByEmail(email);
        PetReminder reminder = petReminderRepository.findByUuidAndUser_IdAndIsActiveTrue(reminderUuid, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("reminder", "uuid", reminderUuid));
        if (request.type() != null) {
            reminder.setType(request.type());
        }
        if (request.dueAt() != null) {
            reminder.setDueAt(request.dueAt());
            reminder.setSentAt(null);
            reminder.setSevenDaySentAt(null);
            reminder.setOneDaySentAt(null);
        }
        if (request.note() != null) {
            reminder.setNote(blankToNull(request.note()));
        }
        if (request.pushEnabled() != null) {
            reminder.setPushEnabled(request.pushEnabled());
        }
        if (request.whatsappEnabled() != null) {
            reminder.setWhatsappEnabled(request.whatsappEnabled());
        }
        if (request.isActive() != null) {
            reminder.setIsActive(request.isActive());
        }
        reminder=petReminderRepository.save(reminder);
        if (Boolean.FALSE.equals(reminder.getIsActive())) {
            cancelAutomation(reminder);
        } else {
            scheduleAutomation(reminder);
        }
        return toModel(reminder);
    }

    @Transactional
    public void delete(String reminderUuid, String email) {
        User user = userDao.userByEmail(email);
        PetReminder reminder = petReminderRepository.findByUuidAndUser_IdAndIsActiveTrue(reminderUuid, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("reminder", "uuid", reminderUuid));
        reminder.setIsActive(false);
        petReminderRepository.save(reminder);
        cancelAutomation(reminder);
    }

    @Transactional
    public int processDueReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<PetReminder> candidates = petReminderRepository.findPendingBetween(
                today.atStartOfDay(), today.plusDays(8).atStartOfDay());
        int sent = 0;
        log.info("Processing due reminders for today: {}", today);
        log.info("============================================================================================================================================================Starting to process due reminders for: {}", candidates.size());
        for (PetReminder reminder : candidates) {
            long daysLeft = ChronoUnit.DAYS.between(today, reminder.getDueAt().toLocalDate());
            boolean oneDay = daysLeft <= 1;
            boolean sevenDay = daysLeft == 7;

            if (oneDay ? reminder.getOneDaySentAt() != null : reminder.getSevenDaySentAt() != null) {
                continue;
            }
            try {
                dispatch(reminder, oneDay ? "tomorrow" : "in " + daysLeft + " days",
                        oneDay ? "ONE_DAY" : "SEVEN_DAY");
                LocalDateTime now = LocalDateTime.now();
                if (oneDay) {
                    reminder.setOneDaySentAt(now);
                    reminder.setSentAt(now);
                } else {
                    reminder.setSevenDaySentAt(now);
                }
                petReminderRepository.save(reminder);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to dispatch reminder {}: {}", reminder.getUuid(), e.getMessage());
            }
            log.info("Finished processing reminder {} for user {}", reminder.getUuid(), reminder.getUser().getEmail());
        }
        return sent + processVaccineReminders(today);
    }

    private int processVaccineReminders(LocalDate today) {
        int sent = 0;
        for (PetVaccineSchedule schedule : petVaccineScheduleRepository.findPendingReminders(today, today.plusDays(7))) {
            long daysLeft = ChronoUnit.DAYS.between(today, schedule.getDueDate());
            boolean oneDay = daysLeft <= 1;
            if (oneDay ? schedule.getOneDaySentAt() != null : schedule.getSevenDaySentAt() != null) {
                continue;
            }
            try {
                String stage = oneDay ? "ONE_DAY" : "SEVEN_DAY";
                if (!dispatchVaccineSchedule(schedule, daysLeft, stage)) {
                    continue;
                }
                if (oneDay) {
                    schedule.setOneDaySentAt(LocalDateTime.now());
                    schedule.setReminderSent(true);
                } else {
                    schedule.setSevenDaySentAt(LocalDateTime.now());
                }
                petVaccineScheduleRepository.save(schedule);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to dispatch vaccine schedule reminder {}: {}", schedule.getId(), e.getMessage(), e);
            }
        }
        return sent;
    }

    private boolean dispatchVaccineSchedule(PetVaccineSchedule schedule, long daysLeft, String stage) {
        Pet pet = schedule.getPet();
        if (pet == null || schedule.getVaccine() == null || pet.getUuid() == null) {
            log.warn("Vaccine schedule {} has no pet or vaccine; reminder not sent", schedule.getId());
            return false;
        }
        User user = userDao.findOptionalByPetUuid(pet.getUuid()).orElse(null);
        List<ClinicPetEnrollment> enrollments = clinicPetEnrollmentRepository.findByPet_IdAndIsActiveTrue(pet.getId());
        ClinicPetEnrollment enrollment = enrollments.size() == 1 ? enrollments.get(0) : null;
        ClinicPetOwner owner = enrollment == null ? null : enrollment.getClinicOwner();
        if (user == null && owner == null) {
            log.warn("Vaccine schedule {} has no unambiguous owner; reminder not sent", schedule.getId());
            return false;
        }
        String email = user != null ? user.getEmail() : owner.getEmail();
        String phone = user != null
                ? (user.getWhatsappNumber() != null && !user.getWhatsappNumber().isBlank()
                        ? user.getWhatsappNumber() : user.getPhoneNumber())
                : owner.getPhone();
        String name = user != null ? user.getFirstName() : owner.getFirstName();
        Clinic clinic = enrollment == null ? null : enrollment.getClinic();
        String petName = pet.getName() == null || pet.getName().isBlank() ? "your pet" : pet.getName();
        String vaccineName = schedule.getVaccine().getName();
        if (vaccineName == null || vaccineName.isBlank()) {
            log.warn("Vaccine schedule {} has no vaccine name; reminder not sent", schedule.getId());
            return false;
        }
        String dueLabel = daysLeft == 0 ? "today" : daysLeft == 1 ? "tomorrow" : "in " + daysLeft + " days";
        String title = "Pet vaccination reminder";
        String message = String.format("%s has a %s vaccination due %s", petName, vaccineName, dueLabel);
        String sourceUuid = "VACCINE_SCHEDULE_" + schedule.getId();
        MessageTenantType tenantType = clinic == null ? null : MessageTenantType.CLINIC;
        Long tenantId = clinic == null ? null : clinic.getId();
        boolean attempted = false;
        if (user != null && email != null && !email.isBlank()) {
            attempted = userService.sendPushNotification(email, title, message);
        }
        if (email != null && !email.isBlank()) {
            if (tenantId == null || automationMessageBridge.canDispatch(
                    tenantType, tenantId, "PET_VACCINE_SCHEDULE", sourceUuid, stage, MessageChannel.EMAIL)) {
                try {
                    attempted |= sendReminderEmail(true, email, name, petName, clinic, vaccineName, title, message,
                            schedule.getDueDate().atStartOfDay());
                    if (tenantId != null) automationMessageBridge.markSent(
                            tenantType, tenantId, "PET_VACCINE_SCHEDULE", sourceUuid, stage, MessageChannel.EMAIL, null);
                } catch (Exception e) {
                    if (tenantId != null) automationMessageBridge.markFailed(
                            tenantType, tenantId, "PET_VACCINE_SCHEDULE", sourceUuid, stage, MessageChannel.EMAIL, "EMAIL_FAILED");
                    throw e;
                }
            }
        }
        if (phone != null && !phone.isBlank() && clinic != null && !whatsAppService.isDeliveryEnabled()) {
            throw new CustomException(
                    "WhatsApp reminder delivery is disabled; reminder will be retried",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (phone != null && !phone.isBlank() && clinic != null) {
            if (tenantId == null || automationMessageBridge.canDispatch(
                    tenantType, tenantId, "PET_VACCINE_SCHEDULE", sourceUuid, stage, MessageChannel.WHATSAPP)) {
                try {
                        String ownerName = name == null || name.isBlank() ? "Pet parent" : name;
                    String clinicName = clinic.getName() == null ? "your clinic" : clinic.getName();
                    outboundMessageService.sendVaccineReminder(phone,
                            List.of(ownerName, petName, clinicName, vaccineName,
                                    schedule.getDueDate().format(EMAIL_DATE)), user, pet);
                    if (tenantId != null) automationMessageBridge.markSent(
                            tenantType, tenantId, "PET_VACCINE_SCHEDULE", sourceUuid, stage, MessageChannel.WHATSAPP, null);
                    attempted = true;
                } catch (Exception e) {
                    if (tenantId != null) automationMessageBridge.markFailed(
                            tenantType, tenantId, "PET_VACCINE_SCHEDULE", sourceUuid, stage, MessageChannel.WHATSAPP, "PROVIDER_REJECTED");
                    throw e;
                }
            }
        }
        if (!attempted) {
            log.warn("Vaccine schedule {} has no available reminder contact or channel", schedule.getId());
        }
        return attempted;
    }

    private void dispatch(PetReminder reminder, String whenLabel, String stage) throws Exception {
        log.info("Dispatching reminder {} for user {}", reminder.getUuid(), reminder.getUser().getEmail());
        User user = reminder.getUser();
        Pet pet = reminder.getPet();
        Clinic clinic = resolveClinic(pet);
        MessageTenantType tenantType = clinic == null ? null : MessageTenantType.CLINIC;
        Long tenantId = clinic == null ? null : clinic.getId();
        String petName = pet != null ? pet.getName() : "your pet";
        String typeLabel = reminder.getType() == null ? "reminder" : reminder.getType().name().toLowerCase();
        String title = "Pet " + typeLabel + " reminder";
        String body = reminder.getNote() != null && !reminder.getNote().isBlank()
                ? reminder.getNote()
                : String.format("%s has a %s due %s", petName, typeLabel, whenLabel);
        if (Boolean.TRUE.equals(reminder.getPushEnabled()) && user.getEmail() != null) {
            userService.sendPushNotification(user.getEmail(), title, body);
        }

        if (tenantId == null || automationMessageBridge.canDispatch(
                tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage, MessageChannel.EMAIL)) {
            try {
                sendEmail(reminder, user, pet, petName, typeLabel);
                if (tenantId != null) automationMessageBridge.markSent(
                        tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage, MessageChannel.EMAIL, null);
            } catch (Exception e) {
                if (tenantId != null) automationMessageBridge.markFailed(
                        tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage, MessageChannel.EMAIL, "EMAIL_FAILED");
            }
        }

        String whatsappPhone = user.getWhatsappNumber() != null && !user.getWhatsappNumber().isBlank()
                ? user.getWhatsappNumber() : user.getPhoneNumber();
        if (Boolean.TRUE.equals(reminder.getWhatsappEnabled()) && whatsappPhone != null
                && !whatsAppService.isDeliveryEnabled()) {
            throw new CustomException(
                    "WhatsApp reminder delivery is disabled; reminder will be retried",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (Boolean.TRUE.equals(reminder.getWhatsappEnabled()) && whatsappPhone != null) {
            if (tenantId == null || automationMessageBridge.canDispatch(
                    tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage, MessageChannel.WHATSAPP)) {
                try {
                    String ownerName = user.getFirstName() == null ? "Pet parent" : user.getFirstName();
                    String clinicName = clinic == null || clinic.getName() == null ? "your clinic" : clinic.getName();
                    String dueDate = reminder.getDueAt().format(EMAIL_DATE);
                    if (reminder.getType() == PetReminderType.VACCINATION || reminder.getType() == PetReminderType.INJECTION) {
                        outboundMessageService.sendVaccineReminder(whatsappPhone,
                                List.of(ownerName, petName, clinicName, typeLabel, dueDate), user, pet);
                    } else if (reminder.getType() == PetReminderType.CHECKUP || reminder.getType() == PetReminderType.VISIT) {
                        String previousVisitDate = reminder.getCreatedAt() == null
                                ? dueDate : reminder.getCreatedAt().format(EMAIL_DATE);
                        outboundMessageService.sendCheckupReminder(whatsappPhone,
                                List.of(ownerName, petName, clinicName, previousVisitDate, dueDate), user, pet);
                    } else {
                        if (!outboundMessageService.trySendAppointmentNotice(whatsappPhone,
                                List.of(ownerName, petName, clinicName, dueDate, dueDate), user, pet)) {
                            if (tenantId != null) automationMessageBridge.markFailed(
                                    tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage,
                                    MessageChannel.WHATSAPP, "TEMPLATE_NOT_CONFIGURED");
                            return;
                        }
                    }
                    if (tenantId != null) automationMessageBridge.markSent(
                            tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage, MessageChannel.WHATSAPP, null);
                } catch (Exception e) {
                    if (tenantId != null) automationMessageBridge.markFailed(
                            tenantType, tenantId, "PET_REMINDER", reminder.getUuid(), stage, MessageChannel.WHATSAPP, "PROVIDER_REJECTED");
                    throw e;
                }
            }
        }
    }

    private void sendEmail(PetReminder reminder, User user, Pet pet, String petName, String typeLabel) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            Clinic clinic = resolveClinic(pet);
            boolean vaccine = reminder.getType() == PetReminderType.VACCINATION
                    || reminder.getType() == PetReminderType.INJECTION;
            boolean hasNote = reminder.getNote() != null && !reminder.getNote().isBlank();
            String reminderName = hasNote ? reminder.getNote() : typeLabel;
            String title = Character.toUpperCase(typeLabel.charAt(0)) + typeLabel.substring(1) + " reminder";
            String message = hasNote ? reminder.getNote()
                    : String.format("%s has a %s due on %s", petName, typeLabel,
                            reminder.getDueAt().format(EMAIL_DATE));
            sendReminderEmail(vaccine, user.getEmail(), user.getFirstName(), petName, clinic, reminderName,
                    title, message, reminder.getDueAt());
        } catch (Exception e) {
            log.warn("Reminder email failed for {}: {}", reminder.getUuid(), e.getMessage());
        }
    }

    private boolean sendReminderEmail(boolean vaccine, String email, String name, String petName, Clinic clinic,
            String reminderName, String title, String message, LocalDateTime dueAt) {
        String time = dueAt.toLocalTime().equals(LocalTime.MIDNIGHT) ? "" : dueAt.format(EMAIL_TIME);
        String address = clinic == null ? null
                : java.util.stream.Stream.of(clinic.getAddress(), clinic.getCity())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(java.util.stream.Collectors.joining(", "));
        return zeptoMailService.sendPetReminderEmail(vaccine, email, name, petName,
                clinic == null ? null : clinic.getName(),
                clinic == null ? null : clinic.getPhone(),
                address, null, reminderName, title, message, dueAt.format(EMAIL_DATE), time,
                clinic == null ? null : clinic.getId());
    }

    private Clinic resolveClinic(Pet pet) {
        if (pet == null) {
            return null;
        }
        List<ClinicPetEnrollment> enrollments = clinicPetEnrollmentRepository.findByPet_IdAndIsActiveTrue(pet.getId());
        return enrollments.size() == 1 ? enrollments.get(0).getClinic() : null;
    }

    private void scheduleAutomation(PetReminder reminder) {
        Clinic clinic = resolveClinic(reminder.getPet());
        if (clinic == null) return;
        MessageTenantType tenantType = MessageTenantType.CLINIC;
        Long tenantId = clinic.getId();
        String petName = reminder.getPet() == null || reminder.getPet().getName() == null
                ? "your pet" : reminder.getPet().getName();
        String typeLabel = reminder.getType() == null ? "reminder" : reminder.getType().name().toLowerCase();
        String body = reminder.getNote() != null && !reminder.getNote().isBlank()
                ? reminder.getNote()
                : String.format("%s has a %s due", petName, typeLabel);
        String recipientName = reminder.getUser() == null ? "" : reminder.getUser().getFirstName();
        String email = reminder.getUser() == null ? null : reminder.getUser().getEmail();
        String phone = reminder.getUser() == null ? null
                : reminder.getUser().getWhatsappNumber() != null
                        && !reminder.getUser().getWhatsappNumber().isBlank()
                                ? reminder.getUser().getWhatsappNumber()
                                : reminder.getUser().getPhoneNumber();
        LocalDateTime sevenAt = reminder.getDueAt().minusDays(7);
        LocalDateTime oneAt = reminder.getDueAt().minusDays(1);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (!sevenAt.toLocalDate().isBefore(today)) {
            scheduleStage(reminder, tenantType, tenantId, "SEVEN_DAY",
                    sevenAt, recipientName, petName, email, phone, body);
        }
        if (!oneAt.toLocalDate().isBefore(today)) {
            scheduleStage(reminder, tenantType, tenantId, "ONE_DAY",
                    oneAt, recipientName, petName, email, phone, body);
        }
    }

    private void scheduleStage(PetReminder reminder, MessageTenantType tenantType, Long tenantId, String stage,
            LocalDateTime scheduledAt, String recipientName, String petName, String email, String phone, String body) {
        if (email != null && !email.isBlank()) {
            automationMessageBridge.schedule(tenantType, tenantId, "PET_REMINDER",
                    "PET_REMINDER", reminder.getUuid(), stage, recipientName, petName,
                    MessageChannel.EMAIL, email, "Pet " + typeLabel(reminder) + " reminder", body, scheduledAt);
        }
        if (Boolean.TRUE.equals(reminder.getWhatsappEnabled()) && phone != null && !phone.isBlank()) {
            automationMessageBridge.schedule(tenantType, tenantId, "PET_REMINDER",
                    "PET_REMINDER", reminder.getUuid(), stage, recipientName, petName,
                    MessageChannel.WHATSAPP, phone, "Pet reminder", body, scheduledAt);
        }
    }

    private void cancelAutomation(PetReminder reminder) {
        Clinic clinic = resolveClinic(reminder.getPet());
        if (clinic == null) return;
        automationMessageBridge.cancelSource(MessageTenantType.CLINIC, clinic.getId(),
                "PET_REMINDER", reminder.getUuid());
    }

    private String typeLabel(PetReminder reminder) {
        return reminder.getType() == null ? "reminder" : reminder.getType().name().toLowerCase();
    }

    private PetReminderModel toModel(PetReminder reminder) {
        return new PetReminderModel(
                reminder.getUuid(),
                reminder.getPet() == null ? null : reminder.getPet().getUuid(),
                reminder.getPet() == null ? null : reminder.getPet().getName(),
                reminder.getType(),
                reminder.getDueAt(),
                reminder.getNote(),
                reminder.getPushEnabled(),
                reminder.getWhatsappEnabled(),
                reminder.getSentAt(),
                reminder.getIsActive());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
