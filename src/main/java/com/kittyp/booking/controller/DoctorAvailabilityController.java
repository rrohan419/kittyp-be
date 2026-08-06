package com.kittyp.booking.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.booking.dto.DoctorAvailabilityModel;
import com.kittyp.booking.dto.DoctorAvailabilityUpdateRequest;
import com.kittyp.booking.entity.DoctorAvailability;
import com.kittyp.booking.repository.DoctorAvailabilityRepository;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DoctorAvailabilityController {

    private static final String INR = "INR";

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorProfileDao doctorProfileDao;
    private final UserDao userDao;
    private final ObjectMapper objectMapper;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.DOCTOR_ME_AVAILABILITY)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<DoctorAvailabilityModel>> getMyAvailability() {
        DoctorProfile doctor = currentDoctor();
        DoctorAvailability row = availabilityRepository.findByDoctor_Id(doctor.getId()).orElse(null);
        return responseBuilder.buildSuccessResponse(toModel(doctor, row), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PutMapping(ApiUrl.DOCTOR_ME_AVAILABILITY)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<DoctorAvailabilityModel>> saveMyAvailability(
            @RequestBody DoctorAvailabilityUpdateRequest request) {
        DoctorProfile doctor = currentDoctor();

        DoctorAvailability row = availabilityRepository.findByDoctor_Id(doctor.getId())
                .orElse(DoctorAvailability.builder().doctor(doctor).build());

        row.setTimezone(request.getTimezone() != null && !request.getTimezone().isBlank()
                ? request.getTimezone()
                : "Asia/Kolkata");
        row.setSlotDurationMinutes(request.getSlotDurationMinutes() != null
                ? request.getSlotDurationMinutes()
                : 30);
        row.setBufferMinutes(request.getBufferMinutes() != null ? request.getBufferMinutes() : 0);
        row.setNotes(request.getNotes());
        row.setWeeklyScheduleJson(writeJson(request.getWeeklySchedule()));
        row.setExceptionsJson(writeJson(request.getExceptions()));

        DoctorAvailability saved = availabilityRepository.save(row);

        doctor.setCurrency(INR);
        if (request.getConsultationFee() != null && request.getConsultationFee() > 0) {
            doctor.setConsultationFee(BigDecimal.valueOf(request.getConsultationFee()));
        } else {
            Double fromSchedule = firstActivePrice(request.getWeeklySchedule());
            if (fromSchedule != null) {
                doctor.setConsultationFee(BigDecimal.valueOf(fromSchedule));
            }
        }
        doctorProfileDao.save(doctor);

        return responseBuilder.buildSuccessResponse(toModel(doctor, saved), ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private DoctorProfile currentDoctor() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userDao.userByEmail(email);
        DoctorProfile doctor = doctorProfileDao.findByUserId(user.getId());
        if (doctor == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        return doctor;
    }

    private DoctorAvailabilityModel toModel(DoctorProfile doctor, DoctorAvailability row) {
        String currency = doctor.getCurrency() != null && !doctor.getCurrency().isBlank()
                ? doctor.getCurrency()
                : INR;
        if (row == null) {
            return new DoctorAvailabilityModel(
                    doctor.getUuid(),
                    currency,
                    30,
                    0,
                    "Asia/Kolkata",
                    null,
                    Collections.emptyList(),
                    Collections.emptyList());
        }
        return new DoctorAvailabilityModel(
                doctor.getUuid(),
                currency,
                row.getSlotDurationMinutes() != null ? row.getSlotDurationMinutes() : 30,
                row.getBufferMinutes() != null ? row.getBufferMinutes() : 0,
                row.getTimezone() != null ? row.getTimezone() : "Asia/Kolkata",
                row.getNotes(),
                readList(row.getWeeklyScheduleJson()),
                readList(row.getExceptionsJson()));
    }

    private String writeJson(List<Map<String, Object>> value) {
        try {
            List<Map<String, Object>> safe = value != null ? value : Collections.emptyList();
            return objectMapper.writeValueAsString(safe);
        } catch (Exception e) {
            throw new CustomException("Failed to serialize availability", HttpStatus.BAD_REQUEST, e);
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Double firstActivePrice(List<Map<String, Object>> schedule) {
        if (schedule == null) {
            return null;
        }
        for (Map<String, Object> slot : schedule) {
            Object active = slot.get("isActive");
            if (active instanceof Boolean b && !b) {
                continue;
            }
            Object price = slot.get("price");
            if (price instanceof Number n) {
                return n.doubleValue();
            }
        }
        return null;
    }
}
