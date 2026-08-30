package com.kittyp.visit.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Weekly doctor hours used by slot listing and booking create. */
final class DoctorHours {

    static final String DEFAULT_ZONE = "Asia/Kolkata";

    private static final Set<String> CLOSED_EXCEPTION_TYPES = Set.of(
            "unavailable", "holiday", "emergency-only");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DateTimeFormatter FLEX_TIME = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .toFormatter();

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

    /**
     * Windows for a calendar date. Closed exceptions (unavailable / holiday / emergency-only)
     * return no windows. reduced-hours replaces the weekly windows for that date.
     */
    static List<LocalTime[]> windowsForDate(String weeklyScheduleJson, String exceptionsJson, LocalDate date) {
        if (date == null) {
            return List.of();
        }
        List<LocalTime[]> reduced = new ArrayList<>();
        boolean reducedHours = false;
        for (Map<String, Object> exception : parseJsonList(exceptionsJson)) {
            if (!dateMatches(exception.get("date"), date)) {
                continue;
            }
            String type = Objects.toString(exception.get("type"), "").trim().toLowerCase(Locale.ROOT);
            if (CLOSED_EXCEPTION_TYPES.contains(type)) {
                return List.of();
            }
            if ("reduced-hours".equals(type)) {
                reducedHours = true;
                LocalTime[] window = parseTimes(exception.get("startTime"), exception.get("endTime"));
                if (window != null) {
                    reduced.add(window);
                }
            }
        }
        if (reducedHours) {
            reduced.sort(Comparator.comparing(w -> w[0]));
            return reduced;
        }
        return windowsOrDefault(weeklyScheduleJson, date.getDayOfWeek());
    }

    static List<LocalTime[]> windowsForDay(String weeklyScheduleJson, DayOfWeek dayOfWeek) {
        List<Map<String, Object>> schedule = parseJsonList(weeklyScheduleJson);
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
                LocalTime from = parseLocalTime(start);
                LocalTime to = parseLocalTime(end);
                if (from == null || to == null) {
                    continue;
                }
                windows.add(new LocalTime[] { from, to });
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

    static boolean hasWeeklySchedule(String weeklyScheduleJson) {
        return !parseJsonList(weeklyScheduleJson).isEmpty();
    }

    private static boolean dateMatches(Object rawDate, LocalDate date) {
        if (rawDate == null) {
            return false;
        }
        String text = rawDate.toString().trim();
        if (text.isEmpty() || "null".equals(text)) {
            return false;
        }
        if (text.length() >= 10) {
            text = text.substring(0, 10);
        }
        try {
            return date.equals(LocalDate.parse(text));
        } catch (Exception e) {
            return false;
        }
    }

    private static LocalTime[] parseTimes(Object startRaw, Object endRaw) {
        String start = Objects.toString(startRaw, null);
        String end = Objects.toString(endRaw, null);
        if (start == null || end == null || "null".equals(start) || "null".equals(end)) {
            return null;
        }
        try {
            LocalTime from = parseLocalTime(start);
            LocalTime to = parseLocalTime(end);
            if (from == null || to == null || to.isBefore(from)) {
                return null;
            }
            return new LocalTime[] { from, to };
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalTime parseLocalTime(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        String text = raw.trim();
        try {
            return LocalTime.parse(text);
        } catch (Exception e) {
            try {
                return LocalTime.parse(text, FLEX_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> list = MAPPER.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }
}
