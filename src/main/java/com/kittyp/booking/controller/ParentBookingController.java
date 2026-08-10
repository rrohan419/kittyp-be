package com.kittyp.booking.controller;

import java.time.LocalDate;
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

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.repository.BookingRepository;
import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.visit.dto.VisitDtos.ParentBookingCreateRequest;
import com.kittyp.visit.service.VisitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ParentBookingController {

    private final BookingRepository bookingRepository;
    private final VisitService visitService;
    private final UserDao userDao;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.USER_BOOKINGS_MINE)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<BookingModel>>> mine() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userDao.userByEmail(email);
        List<BookingModel> models = bookingRepository.findByOwner_IdOrderBySlotStartDesc(user.getId()).stream()
                .map(this::toModel)
                .toList();
        return responseBuilder.buildSuccessResponse(models, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_BOOKINGS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<BookingModel>> create(@RequestBody @Valid ParentBookingCreateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        BookingModel model = visitService.createParentBooking(request, email);
        return responseBuilder.buildSuccessResponse(model, ResponseMessage.SUCCESS, HttpStatus.CREATED);
    }

    @GetMapping(ApiUrl.USER_DOCTOR_SLOTS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<String>>> slots(
            @PathVariable String clinicUuid,
            @PathVariable String doctorUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<String> slots = visitService.listParentDoctorSlots(clinicUuid, doctorUuid, date, email).stream()
                .map(Object::toString)
                .toList();
        return responseBuilder.buildSuccessResponse(slots, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private BookingModel toModel(Booking booking) {
        String ownerName = booking.getOwner() == null ? null
                : ((booking.getOwner().getFirstName() == null ? "" : booking.getOwner().getFirstName()) + " "
                        + (booking.getOwner().getLastName() == null ? "" : booking.getOwner().getLastName())).trim();
        String petName = booking.getPet() == null ? "Pet" : booking.getPet().getName();
        String petUuid = booking.getPet() == null ? null : booking.getPet().getUuid();
        String doctorName = null;
        String doctorSpecialization = null;
        String doctorPhotoUrl = null;
        if (booking.getDoctor() != null) {
            doctorPhotoUrl = booking.getDoctor().getPhotoUrl();
            if (booking.getDoctor().getSpecialization() != null) {
                doctorSpecialization = booking.getDoctor().getSpecialization().name();
            }
            if (booking.getDoctor().getUser() != null) {
                doctorName = ((booking.getDoctor().getUser().getFirstName() == null ? ""
                        : booking.getDoctor().getUser().getFirstName())
                        + " "
                        + (booking.getDoctor().getUser().getLastName() == null ? ""
                                : booking.getDoctor().getUser().getLastName())).trim();
                if (doctorName.isBlank()) {
                    doctorName = null;
                }
            }
        }
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
                booking.getClinic() == null ? null : booking.getClinic().getName(),
                doctorName,
                doctorSpecialization,
                doctorPhotoUrl);
    }
}
