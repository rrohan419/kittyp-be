package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class DoctorHoursTest {

    private static final String MON_FRI = """
            [
              {"dayOfWeek":1,"startTime":"09:00","endTime":"17:00","isActive":true},
              {"dayOfWeek":5,"startTime":"10:00","endTime":"14:00","isActive":true}
            ]
            """;

    @Test
    void unconfiguredScheduleDefaultsToNineToSix() {
        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(null, DayOfWeek.WEDNESDAY);
        assertEquals(1, windows.size());
        assertEquals(LocalTime.of(9, 0), windows.get(0)[0]);
        assertEquals(LocalTime.of(18, 0), windows.get(0)[1]);
    }

    @Test
    void configuredOffDayHasNoWindows() {
        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(MON_FRI, DayOfWeek.TUESDAY);
        assertTrue(windows.isEmpty());
        assertFalse(DoctorHours.fitsWindow(windows, LocalTime.of(11, 0), LocalTime.of(11, 30)));
    }

    @Test
    void slotInsideMondayHoursFits() {
        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(MON_FRI, DayOfWeek.MONDAY);
        assertTrue(DoctorHours.fitsWindow(windows, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        assertTrue(DoctorHours.fitsWindow(windows, LocalTime.of(16, 30), LocalTime.of(17, 0)));
    }

    @Test
    void slotOutsideMondayHoursRejected() {
        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(MON_FRI, DayOfWeek.MONDAY);
        assertFalse(DoctorHours.fitsWindow(windows, LocalTime.of(8, 0), LocalTime.of(8, 30)));
        assertFalse(DoctorHours.fitsWindow(windows, LocalTime.of(17, 0), LocalTime.of(17, 30)));
        assertFalse(DoctorHours.fitsWindow(windows, LocalTime.of(21, 0), LocalTime.of(21, 30)));
    }

    @Test
    void sundayZeroMapsToJavaSunday() {
        String sunday = """
                [{"dayOfWeek":0,"startTime":"11:00","endTime":"13:00","isActive":true}]
                """;
        List<LocalTime[]> windows = DoctorHours.windowsOrDefault(sunday, DayOfWeek.SUNDAY);
        assertTrue(DoctorHours.fitsWindow(windows, LocalTime.of(11, 0), LocalTime.of(11, 30)));
        assertFalse(DoctorHours.fitsWindow(windows, LocalTime.of(14, 0), LocalTime.of(14, 30)));
    }

    @Test
    void inactiveDayIsClosed() {
        String inactiveMonday = """
                [{"dayOfWeek":1,"startTime":"09:00","endTime":"17:00","isActive":false}]
                """;
        assertTrue(DoctorHours.hasWeeklySchedule(inactiveMonday));
        assertTrue(DoctorHours.windowsOrDefault(inactiveMonday, DayOfWeek.MONDAY).isEmpty());
    }
}
