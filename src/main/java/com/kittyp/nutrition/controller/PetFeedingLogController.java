package com.kittyp.nutrition.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.nutrition.dao.PetFeedingLogDao;
import com.kittyp.nutrition.dto.PetFeedingLogRequest;
import com.kittyp.nutrition.entity.PetFeedingLog;
import com.kittyp.nutrition.model.PetFeedingLogModel;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.user.service.PetAccessGuard;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PetFeedingLogController {

    private final ApiResponse<?> responseBuilder;
    private final PetFeedingLogDao petFeedingLogDao;
    private final UserDao userDao;
    private final PetAccessGuard petAccessGuard;

    @GetMapping(ApiUrl.PET_FEEDING_LOGS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<PetFeedingLogModel>>> feedingLogs(
            @PathVariable String petUuid,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        petAccessGuard.requirePetAccess(currentUser(), petUuid);
        LocalDateTime start = from == null ? LocalDate.now().minusDays(30).atStartOfDay() : from;
        LocalDateTime end = to == null ? LocalDateTime.now() : to;
        if (end.isBefore(start)) {
            throw new CustomException("'to' must not be before 'from'", HttpStatus.BAD_REQUEST);
        }
        List<PetFeedingLogModel> logs = petFeedingLogDao.findByPetUuidBetween(petUuid, start, end).stream()
                .map(this::toModel)
                .toList();
        return responseBuilder.buildSuccessResponse(logs, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.PET_FEEDING_LOGS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetFeedingLogModel>> addFeedingLog(
            @PathVariable String petUuid,
            @RequestBody @Valid PetFeedingLogRequest request) {
        User user = currentUser();
        petAccessGuard.requireOwner(user, petUuid);
        PetFeedingLog log = PetFeedingLog.builder()
                .petUuid(petUuid)
                .userUuid(user.getUuid())
                .dailyPlanId(request.dailyPlanId())
                .status(request.status())
                .consumedQuantityInGrams(request.quantity())
                .notes(request.notes())
                .loggedAt(request.loggedAt() == null ? LocalDateTime.now() : request.loggedAt())
                .build();
        return responseBuilder.buildSuccessResponse(toModel(petFeedingLogDao.save(log)),
                ResponseMessage.SUCCESS, HttpStatus.CREATED);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDao.userByEmail(email);
    }

    private PetFeedingLogModel toModel(PetFeedingLog log) {
        return new PetFeedingLogModel(log.getId(), log.getDailyPlanId(), log.getStatus(),
                log.getConsumedQuantityInGrams(), log.getNotes(), log.getLoggedAt());
    }
}
