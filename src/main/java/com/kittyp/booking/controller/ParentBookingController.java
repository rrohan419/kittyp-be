package com.kittyp.booking.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.booking.dto.VideoJoinModel;
import com.kittyp.booking.service.BookingVideoService;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.visit.dto.VisitDtos.ParentBookingCreateRequest;
import com.kittyp.visit.service.VisitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ParentBookingController {

    private final VisitService visitService;
    private final BookingVideoService bookingVideoService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.USER_BOOKINGS_MINE)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<BookingModel>>> mine() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(visitService.listMyParentBookings(email), ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_BOOKINGS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<BookingModel>> create(@RequestBody @Valid ParentBookingCreateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        BookingModel model = visitService.createParentBooking(request, email);
        return responseBuilder.buildSuccessResponse(model, ResponseMessage.SUCCESS, HttpStatus.CREATED);
    }

    @GetMapping(ApiUrl.USER_BOOKING_VIDEO)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<VideoJoinModel>> video(@PathVariable String bookingUuid) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(bookingVideoService.join(email, bookingUuid),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.USER_DOCTOR_SLOTS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<String>>> slots(
            @PathVariable String clinicUuid,
            @PathVariable String doctorUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ok(visitService.listParentDoctorSlots(clinicUuid, doctorUuid, date, email));
    }

    /**
     * Canonical: {@code /public/clinics/.../slots}. Other patterns are compat aliases.
     * Date is the clinic-local calendar day; results are local slot starts (no offset).
     */
    @GetMapping({ ApiUrl.PUBLIC_CLINIC_DOCTOR_SLOTS, ApiUrl.PUBLIC_CLINIC_DOCTOR_SLOTS_COMPAT,
            ApiUrl.CLINICS_DOCTOR_SLOTS_COMPAT, ApiUrl.CLINIC_DOCTOR_SLOTS_COMPAT })
    public ResponseEntity<SuccessResponse<List<String>>> publicSlots(
            @PathVariable String clinicUuid,
            @PathVariable String doctorUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ok(visitService.listDoctorSlots(clinicUuid, doctorUuid, date));
    }

    /** Compat alias. {@code clinicUuid} and {@code date} are required (missing → 400). */
    @GetMapping(ApiUrl.DOCTOR_SLOTS_COMPAT)
    public ResponseEntity<SuccessResponse<List<String>>> doctorSlots(
            @PathVariable String doctorUuid,
            @RequestParam String clinicUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ok(visitService.listDoctorSlots(clinicUuid, doctorUuid, date));
    }

    private ResponseEntity<SuccessResponse<List<String>>> ok(List<LocalDateTime> slots) {
        return responseBuilder.buildSuccessResponse(
                slots.stream().map(Object::toString).toList(), ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
