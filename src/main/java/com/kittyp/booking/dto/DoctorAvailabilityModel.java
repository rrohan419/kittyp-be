package com.kittyp.booking.dto;

import java.util.List;
import java.util.Map;

public record DoctorAvailabilityModel(
        String doctorUuid,
        String currency,
        Integer slotDurationMinutes,
        Integer bufferMinutes,
        String timezone,
        String notes,
        List<Map<String, Object>> weeklySchedule,
        List<Map<String, Object>> exceptions) {
}
