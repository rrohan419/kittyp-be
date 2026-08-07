package com.kittyp.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ParentBookingController {

    private final BookingRepository bookingRepository;
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

    private BookingModel toModel(Booking booking) {
        String ownerName = booking.getOwner() == null ? null
                : ((booking.getOwner().getFirstName() == null ? "" : booking.getOwner().getFirstName()) + " "
                        + (booking.getOwner().getLastName() == null ? "" : booking.getOwner().getLastName())).trim();
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
                booking.getNotes());
    }
}
