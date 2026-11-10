package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.notification.service.OutboundMessageService;
import com.kittyp.notification.service.WhatsAppService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.PetReminder;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.PetReminderType;
import com.kittyp.user.repository.PetReminderRepository;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.vaccine.entity.PetVaccineSchedule;
import com.kittyp.vaccine.entity.VaccineMaster;
import com.kittyp.vaccine.repository.PetVaccineScheduleRepository;

@ExtendWith(MockitoExtension.class)
class PetReminderServiceVaccineTest {
    @Mock private ZeptoMailService mail;
    @Mock private PetReminderRepository reminders;
    @Mock private PetVaccineScheduleRepository schedules;
    @Mock private PetsRepository pets;
    @Mock private ClinicPetEnrollmentRepository enrollments;
    @Mock private UserDao users;
    @Mock private PetAccessGuard accessGuard;
    @Mock private UserService userService;
    @Mock private OutboundMessageService outbound;
    @Mock private WhatsAppService whatsApp;
    @InjectMocks private PetReminderService service;

    @Test
    void sendsBothStagesOnceWithVaccineAndDate() {
        PetVaccineSchedule schedule = schedule(LocalDate.now().plusDays(7));
        User user = User.builder().email("parent@example.com").firstName("Pat").build();
        when(schedules.findPendingReminders(any(), any())).thenReturn(List.of(schedule));
        when(users.findOptionalByPetUuid("pet-1")).thenReturn(Optional.of(user));
        when(userService.sendPushNotification(anyString(), anyString(), anyString())).thenReturn(true);
        when(mail.sendPetReminderEmail(anyBoolean(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertEquals(1, service.processDueReminders());
        assertNotNull(schedule.getSevenDaySentAt());
        assertNull(schedule.getOneDaySentAt());
        assertFalse(schedule.getReminderSent());
        assertEquals(0, service.processDueReminders());
        verify(schedules, times(2)).findPendingReminders(LocalDate.now(), LocalDate.now().plusDays(7));
        verify(userService).sendPushNotification(eq("parent@example.com"), anyString(),
                eq("Milo has a Rabies vaccination due in 7 days"));
        verify(mail).sendPetReminderEmail(true, "parent@example.com", "Pat", "Milo", null, null, null, null,
                "Rabies", "Pet vaccination reminder", "Milo has a Rabies vaccination due in 7 days",
                schedule.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), "");

        schedule.setDueDate(LocalDate.now().plusDays(1));
        assertEquals(1, service.processDueReminders());
        assertNotNull(schedule.getOneDaySentAt());
        assertTrue(schedule.getReminderSent());
        assertEquals(0, service.processDueReminders());
        verify(userService).sendPushNotification(eq("parent@example.com"), anyString(),
                eq("Milo has a Rabies vaccination due tomorrow"));
    }

    @Test
    void sendsCatchUpReminderForNewScheduleInsideSevenDays() {
        PetVaccineSchedule schedule = schedule(LocalDate.now().plusDays(3));
        User user = User.builder().email("parent@example.com").build();
        when(schedules.findPendingReminders(any(), any())).thenReturn(List.of(schedule));
        when(users.findOptionalByPetUuid("pet-1")).thenReturn(Optional.of(user));
        when(userService.sendPushNotification(anyString(), anyString(), anyString())).thenReturn(true);

        assertEquals(1, service.processDueReminders());
        verify(userService).sendPushNotification(anyString(), anyString(),
                eq("Milo has a Rabies vaccination due in 3 days"));
        assertNotNull(schedule.getSevenDaySentAt());
    }

    @Test
    void sendsFinalReminderOnDueDate() {
        PetVaccineSchedule schedule = schedule(LocalDate.now());
        User user = User.builder().email("parent@example.com").build();
        when(schedules.findPendingReminders(any(), any())).thenReturn(List.of(schedule));
        when(users.findOptionalByPetUuid("pet-1")).thenReturn(Optional.of(user));
        when(userService.sendPushNotification(anyString(), anyString(), anyString())).thenReturn(true);

        assertEquals(1, service.processDueReminders());
        verify(userService).sendPushNotification(anyString(), anyString(),
                eq("Milo has a Rabies vaccination due today"));
        assertNotNull(schedule.getOneDaySentAt());
        assertTrue(schedule.getReminderSent());
    }

    @Test
    void sendsToUnlinkedClinicOwnerUsingTheirClinic() {
        PetVaccineSchedule schedule = schedule(LocalDate.now().plusDays(1));
        Clinic clinic = Clinic.builder().name("Clinic").whatsappToken("token")
                .whatsappPhoneNumberId("phone-id").build();
        ClinicPetOwner owner = ClinicPetOwner.builder().email("owner@example.com").firstName("Pat")
                .phone("1234567890").build();
        ClinicPetEnrollment enrollment = ClinicPetEnrollment.builder().clinic(clinic).clinicOwner(owner).build();
        when(schedules.findPendingReminders(any(), any())).thenReturn(List.of(schedule));
        when(enrollments.findByPet_IdAndIsActiveTrue(1L)).thenReturn(List.of(enrollment));
        when(whatsApp.isDeliveryEnabled()).thenReturn(true);
        when(mail.sendPetReminderEmail(anyBoolean(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertEquals(1, service.processDueReminders());
        verify(userService, never()).sendPushNotification(any(), any(), any());
        verify(outbound).sendVaccineReminder(any(), eq("1234567890"), eq(List.of("Milo", "Rabies",
                schedule.getDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))),
                eq(null), eq(schedule.getPet()));
        assertTrue(schedule.getReminderSent());
    }

    @Test
    void doesNotMarkSentWhenNoChannelIsAvailable() {
        PetVaccineSchedule schedule = schedule(LocalDate.now().plusDays(1));
        User user = User.builder().email("parent@example.com").build();
        when(schedules.findPendingReminders(any(), any())).thenReturn(List.of(schedule));
        when(users.findOptionalByPetUuid("pet-1")).thenReturn(Optional.of(user));

        assertEquals(0, service.processDueReminders());
        assertNull(schedule.getOneDaySentAt());
        assertFalse(schedule.getReminderSent());
        verify(schedules, never()).save(any());
    }

    @Test
    void leavesAmbiguousOwnerAndFailedSendPending() {
        PetVaccineSchedule ambiguous = schedule(LocalDate.now().plusDays(1));
        PetVaccineSchedule failing = schedule(LocalDate.now().plusDays(1));
        failing.getPet().setUuid("pet-2");
        failing.getPet().setId(2L);
        User user = User.builder().email("parent@example.com").build();
        when(schedules.findPendingReminders(any(), any())).thenReturn(List.of(ambiguous, failing));
        when(enrollments.findByPet_IdAndIsActiveTrue(1L)).thenReturn(List.of(
                ClinicPetEnrollment.builder().build(), ClinicPetEnrollment.builder().build()));
        when(users.findOptionalByPetUuid("pet-2")).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("push failed")).when(userService)
                .sendPushNotification(anyString(), anyString(), anyString());

        assertEquals(0, service.processDueReminders());
        assertNull(ambiguous.getOneDaySentAt());
        assertNull(failing.getOneDaySentAt());
        verify(schedules, never()).save(any());
    }

    @Test
    void stillProcessesOwnerCreatedReminders() {
        User user = User.builder().email("parent@example.com").build();
        Pet pet = Pet.builder().name("Milo").build();
        PetReminder reminder = PetReminder.builder().uuid("reminder-1").user(user).pet(pet)
                .type(PetReminderType.CHECKUP).dueAt(LocalDateTime.now().plusDays(1))
                .pushEnabled(true).whatsappEnabled(false).build();
        when(reminders.findPendingBetween(any(), any())).thenReturn(List.of(reminder));

        assertEquals(1, service.processDueReminders());
        assertNotNull(reminder.getOneDaySentAt());
        verify(reminders).save(reminder);
        verify(schedules).findPendingReminders(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    private static PetVaccineSchedule schedule(LocalDate dueDate) {
        Pet pet = Pet.builder().uuid("pet-1").name("Milo").build();
        pet.setId(1L);
        PetVaccineSchedule schedule = PetVaccineSchedule.builder().pet(pet)
                .vaccine(VaccineMaster.builder().name("Rabies").build()).dueDate(dueDate).build();
        schedule.setId(10L);
        return schedule;
    }
}
