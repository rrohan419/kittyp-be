package com.kittyp.visit.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Weekly doctor hours used by slot listing and booking create. */
final class DoctorHours {

    static final String DEFAULT_ZONE = "Asia/Kolkata";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DoctorHours() {
    }

    /** Slot wall-clock times are clinic-local; never compare them to JVM/UTC now. */
    static ZoneId zoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_ZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    static LocalDateTime nowLocal(String timezone) {
        return ZonedDateTime.now(zoneId(timezone)).toLocalDateTime();
    }

    record BusyRange(LocalDateTime start, LocalDateTime end) {
    }

    /**
     * Free slot starts on {@code day} that have not begun yet in clinic-local time
     * and do not overlap busy ranges.
     */
    static List<LocalDateTime> freeSlotStarts(LocalDate day, List<LocalTime[]> windows, int durationMinutes,
            LocalDateTime nowClinicLocal, List<BusyRange> busy) {
        if (day == null || windows == null || windows.isEmpty() || durationMinutes <= 0 || nowClinicLocal == null) {
            return List.of();
        }
        if (day.isBefore(nowClinicLocal.toLocalDate())) {
            return List.of();
        }
        List<BusyRange> busySafe = busy == null ? List.of() : busy;
        List<LocalDateTime> free = new ArrayList<>();
        for (LocalTime[] window : windows) {
            if (window == null || window.length < 2 || window[0] == null || window[1] == null) {
                continue;
            }
            LocalDateTime cursor = day.atTime(window[0]);
            LocalDateTime windowEnd = day.atTime(window[1]);
            while (!cursor.plusMinutes(durationMinutes).isAfter(windowEnd)) {
                LocalDateTime slotEnd = cursor.plusMinutes(durationMinutes);
                if (!cursor.isBefore(nowClinicLocal)) {
                    LocalDateTime start = cursor;
                    boolean conflict = busySafe.stream()
                            .anyMatch(b -> b.start() != null && b.end() != null
                                    && b.start().isBefore(slotEnd) && b.end().isAfter(start));
                    if (!conflict) {
                        free.add(start);
                    }
                }
                cursor = cursor.plusMinutes(durationMinutes);
            }
        }
        return free;
    }

    static boolean hasWeeklySchedule(String weeklyScheduleJson) {
        return !parseSchedule(weeklyScheduleJson).isEmpty();
    }

    /**
     * Windows for that weekday. Empty schedule (never configured) falls back to 09:00–18:00.
     * Configured schedule with no window that day means the doctor is closed.
     */
    static List<LocalTime[]> windowsOrDefault(String weeklyScheduleJson, DayOfWeek dayOfWeek) {
        List<LocalTime[]> windows = windowsForDay(weeklyScheduleJson, dayOfWeek);
        if (windows.isEmpty() && !hasWeeklySchedule(weeklyScheduleJson)) {
            return List.<LocalTime[]>of(new LocalTime[] { LocalTime.of(9, 0), LocalTime.of(18, 0) });
        }
        return windows;
    }

    static List<LocalTime[]> windowsForDay(String weeklyScheduleJson, DayOfWeek dayOfWeek) {
        List<Map<String, Object>> schedule = parseSchedule(weeklyScheduleJson);
        if (schedule.isEmpty()) {
            return List.of();
        }
        int dow = dayOfWeek.getValue(); // Monday=1 … Sunday=7
        List<LocalTime[]> windows = new ArrayList<>();
        for (Map<String, Object> slot : schedule) {
            Object active = slot.get("isActive");
            if (active instanceof Boolean b && !b) {
                continue;
            }
            Object dayVal = slot.get("dayOfWeek");
            if (dayVal == null) {
                continue;
            }
            int slotDay;
            try {
                slotDay = dayVal instanceof Number n ? n.intValue() : Integer.parseInt(dayVal.toString());
            } catch (NumberFormatException e) {
                continue;
            }
            if (slotDay != dow && !(slotDay == 0 && dow == 7)) {
                continue;
            }
            String start = Objects.toString(slot.get("startTime"), null);
            String end = Objects.toString(slot.get("endTime"), null);
            if (start == null || end == null || "null".equals(start) || "null".equals(end)) {
                continue;
            }
            try {
                windows.add(new LocalTime[] { LocalTime.parse(start), LocalTime.parse(end) });
            } catch (Exception e) {
                continue;
            }
        }
        windows.sort(Comparator.comparing(w -> w[0]));
        return windows;
    }

    /** True when [start, end] sits fully inside at least one window. */
    static boolean fitsWindow(List<LocalTime[]> windows, LocalTime start, LocalTime end) {
        if (windows == null || windows.isEmpty() || start == null || end == null || end.isBefore(start)) {
            return false;
        }
        for (LocalTime[] window : windows) {
            if (!start.isBefore(window[0]) && !end.isAfter(window[1])) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, Object>> parseSchedule(String weeklyScheduleJson) {
        if (weeklyScheduleJson == null || weeklyScheduleJson.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> schedule = MAPPER.readValue(weeklyScheduleJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            return schedule == null ? List.of() : schedule;
        } catch (Exception e) {
            return List.of();
        }
    }
}
