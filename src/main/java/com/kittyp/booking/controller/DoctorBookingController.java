package com.kittyp.booking.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
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
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.service.VisitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DoctorBookingController {

    private final BookingRepository bookingRepository;
    private final DoctorProfileDao doctorProfileDao;
    private final UserDao userDao;
    private final VisitService visitService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.DOCTOR_BOOKINGS_MINE)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<List<BookingModel>>> mine(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String clinicUuid) {
        DoctorProfile doctor = currentDoctor();
        LocalDate start;
        LocalDate end;
        if (from != null || to != null) {
            start = from == null ? LocalDate.now() : from;
            end = to == null ? start : to;
        } else {
            start = date == null ? LocalDate.now() : date;
            end = start;
        }
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        LocalDateTime rangeFrom = start.atStartOfDay();
        LocalDateTime rangeTo = end.atTime(LocalTime.MAX);
        List<BookingModel> models = bookingRepository
                .findByDoctor_IdAndSlotStartBetweenOrderBySlotStartAsc(doctor.getId(), rangeFrom, rangeTo)
                .stream()
                .filter(b -> clinicUuid == null || clinicUuid.isBlank()
                        || (b.getClinic() != null && clinicUuid.equals(b.getClinic().getUuid())))
                .map(this::toModel)
                .toList();
        return responseBuilder.buildSuccessResponse(models, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.DOCTOR_BOOKING_START_TREATMENT)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<VisitModel>> startTreatment(@PathVariable String bookingUuid) {
        return responseBuilder.buildSuccessResponse(
                visitService.startTreatmentFromBooking(bookingUuid, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    private BookingModel toModel(Booking booking) {
        String ownerName = null;
        if (booking.getOwner() != null) {
            ownerName = ((booking.getOwner().getFirstName() == null ? "" : booking.getOwner().getFirstName()) + " "
                    + (booking.getOwner().getLastName() == null ? "" : booking.getOwner().getLastName())).trim();
            if (ownerName.isBlank()) {
                ownerName = null;
            }
        }
        if (ownerName == null && booking.getPet() != null && booking.getPet().getClinicOwner() != null) {
            var co = booking.getPet().getClinicOwner();
            String last = co.getLastName() == null ? "" : co.getLastName().trim();
            ownerName = ((co.getFirstName() == null ? "" : co.getFirstName())
                    + (last.isEmpty() ? "" : " " + last)).trim();
            if (ownerName.isBlank()) {
                ownerName = null;
            }
        }
        String petName = booking.getPet() == null ? "Pet" : booking.getPet().getName();
        String petUuid = booking.getPet() == null ? null : booking.getPet().getUuid();
        return new BookingModel(
                booking.getUuid(),
                petUuid,
                petName,
                ownerName,
                booking.getDoctor() == null ? null : booking.getDoctor().getUuid(),
                booking.getSlotStart(),
                booking.getSlotEnd(),
                booking.getTimezone(),
                booking.getStatus(),
                booking.getMode() == null ? null : booking.getMode().name(),
                booking.getNotes(),
                booking.getClinic() == null ? null : booking.getClinic().getUuid(),
                booking.getClinic() == null ? null : booking.getClinic().getName());
    }

    private DoctorProfile currentDoctor() {
        String email = email();
        User user = userDao.userByEmail(email);
        DoctorProfile doctor = doctorProfileDao.findByUserId(user.getId());
        if (doctor == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        return doctor;
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
