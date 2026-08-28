package com.kittyp.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.user.dto.PetReminderDtos.PetReminderModel;
import com.kittyp.user.dto.PetReminderDtos.PetReminderRequest;
import com.kittyp.user.dto.PetReminderDtos.PetReminderUpdateRequest;
import com.kittyp.user.service.PetReminderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PetReminderController {

    private final PetReminderService petReminderService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.USER_REMINDERS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<PetReminderModel>>> list() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(petReminderService.listMine(email), ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_REMINDERS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetReminderModel>> create(@RequestBody @Valid PetReminderRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(petReminderService.create(request, email), ResponseMessage.SUCCESS,
                HttpStatus.CREATED);
    }

    @PatchMapping(ApiUrl.USER_REMINDER_BY_UUID)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetReminderModel>> update(@PathVariable String reminderUuid,
            @RequestBody PetReminderUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return responseBuilder.buildSuccessResponse(petReminderService.update(reminderUuid, request, email),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @DeleteMapping(ApiUrl.USER_REMINDER_BY_UUID)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable String reminderUuid) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        petReminderService.delete(reminderUuid, email);
        return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
