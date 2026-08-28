package com.kittyp.booking.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorAvailabilityUpdateRequest {

    private Integer slotDurationMinutes;
    private Integer bufferMinutes;
    private String timezone;
    private String notes;
    /** Default consultation fee in INR applied to doctor profile. */
    private Double consultationFee;
    private List<Map<String, Object>> weeklySchedule;
    private List<Map<String, Object>> exceptions;
}
