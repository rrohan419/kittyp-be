package com.kittyp.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.notification.service.OutboundMessageService;
import com.kittyp.notification.service.WhatsAppSenderCredentials;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetReminderService {

    private final PetReminderRepository petReminderRepository;
    private final PetsRepository petsRepository;
    private final UserDao userDao;
    private final PetAccessGuard petAccessGuard;
    private final UserService userService;
    private final OutboundMessageService outboundMessageService;

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
        return toModel(petReminderRepository.save(reminder));
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
        return toModel(petReminderRepository.save(reminder));
    }

    @Transactional
    public void delete(String reminderUuid, String email) {
        User user = userDao.userByEmail(email);
        PetReminder reminder = petReminderRepository.findByUuidAndUser_IdAndIsActiveTrue(reminderUuid, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("reminder", "uuid", reminderUuid));
        reminder.setIsActive(false);
        petReminderRepository.save(reminder);
    }

    @Transactional
    public int processDueReminders() {
        List<PetReminder> due = petReminderRepository.findDueUnsent(LocalDateTime.now());
        int sent = 0;
        for (PetReminder reminder : due) {
            try {
                dispatch(reminder);
                reminder.setSentAt(LocalDateTime.now());
                petReminderRepository.save(reminder);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to dispatch reminder {}: {}", reminder.getUuid(), e.getMessage());
            }
        }
        return sent;
    }

    private void dispatch(PetReminder reminder) {
        User user = reminder.getUser();
        Pet pet = reminder.getPet();
        String petName = pet != null ? pet.getName() : "your pet";
        String typeLabel = reminder.getType() == null ? "reminder" : reminder.getType().name().toLowerCase();
        String title = "Pet " + typeLabel + " reminder";
        String body = reminder.getNote() != null && !reminder.getNote().isBlank()
                ? reminder.getNote()
                : String.format("%s has a %s due now", petName, typeLabel);

        if (Boolean.TRUE.equals(reminder.getPushEnabled()) && user.getEmail() != null) {
            userService.sendPushNotification(user.getEmail(), title, body);
        }

        if (Boolean.TRUE.equals(reminder.getWhatsappEnabled()) && user.getPhoneNumber() != null) {
            WhatsAppSenderCredentials sender = resolveSender(pet);
            if (sender.isConfigured()) {
                if (reminder.getType() == PetReminderType.VACCINATION || reminder.getType() == PetReminderType.INJECTION) {
                    outboundMessageService.sendVaccineReminder(sender, user.getPhoneNumber(),
                            List.of(petName, typeLabel), user, pet);
                } else if (reminder.getType() == PetReminderType.CHECKUP || reminder.getType() == PetReminderType.VISIT) {
                    outboundMessageService.sendCheckupReminder(sender, user.getPhoneNumber(),
                            List.of(petName, typeLabel), user, pet);
                } else {
                    outboundMessageService.trySendAppointmentNotice(sender, user.getPhoneNumber(),
                            List.of(petName, typeLabel, reminder.getDueAt().toString()), user, pet);
                }
            }
        }
    }

    private WhatsAppSenderCredentials resolveSender(Pet pet) {
        Clinic clinic = pet != null ? pet.getClinic() : null;
        if (clinic != null) {
            WhatsAppSenderCredentials clinicSender = WhatsAppSenderCredentials.of(
                    clinic.getWhatsappToken(), clinic.getWhatsappPhoneNumberId());
            if (clinicSender.isConfigured()) {
                return clinicSender;
            }
        }
        return WhatsAppSenderCredentials.of(null, null);
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
