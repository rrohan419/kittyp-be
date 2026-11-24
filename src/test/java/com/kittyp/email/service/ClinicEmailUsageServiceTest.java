package com.kittyp.email.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.email.entity.ClinicEmailActivity;
import com.kittyp.email.repository.ClinicEmailActivityRepository;

@ExtendWith(MockitoExtension.class)
class ClinicEmailUsageServiceTest {
    @Mock ClinicRepository clinics;
    @Mock ClinicEmailActivityRepository activities;
    @InjectMocks ClinicEmailUsageService service;

    @Test
    void reservesCreditUntilProviderResultIsKnown() {
        when(clinics.lockById(3L)).thenReturn(Optional.of(new Clinic()));
        when(activities.countByClinic_IdAndPeriodStartAndStatusIn(anyLong(), any(), anyList())).thenReturn(1_999L);
        when(activities.save(any(ClinicEmailActivity.class))).thenAnswer(invocation -> {
            ClinicEmailActivity activity = invocation.getArgument(0);
            activity.setId(11L);
            return activity;
        });

        ClinicEmailUsageService.Reservation reservation =
                service.reserve(3L, "owner@example.com", "invoice");

        assertTrue(reservation.allowed());
        assertEquals(11L, reservation.activityId());
        ArgumentCaptor<ClinicEmailActivity> activityCaptor = ArgumentCaptor.forClass(ClinicEmailActivity.class);
        verify(activities).save(activityCaptor.capture());
        assertEquals(ClinicEmailActivity.Status.RESERVED, activityCaptor.getValue().getStatus());
        assertEquals("invoice", activityCaptor.getValue().getEmailType());
    }

    @Test
    void recordsBlockedEmailWithoutReservingOrConsumingCreditAtLimit() {
        when(clinics.lockById(3L)).thenReturn(Optional.of(new Clinic()));
        when(activities.countByClinic_IdAndPeriodStartAndStatusIn(anyLong(), any(), anyList())).thenReturn(2_000L);
        when(activities.save(any(ClinicEmailActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicEmailUsageService.Reservation reservation =
                service.reserve(3L, "owner@example.com", "invoice");

        assertFalse(reservation.allowed());
        assertNull(reservation.activityId());
        ArgumentCaptor<ClinicEmailActivity> activityCaptor = ArgumentCaptor.forClass(ClinicEmailActivity.class);
        verify(activities).save(activityCaptor.capture());
        assertEquals(ClinicEmailActivity.Status.BLOCKED, activityCaptor.getValue().getStatus());
    }
}
