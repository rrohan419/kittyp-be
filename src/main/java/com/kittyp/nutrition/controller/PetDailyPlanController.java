package com.kittyp.nutrition.controller;

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
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.nutrition.dto.PetDailyPlanDto;
import com.kittyp.nutrition.model.PetDailyPlanModel;
import com.kittyp.nutrition.service.PetDailyPlanService;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.user.service.PetAccessGuard;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PetDailyPlanController {

    private final ApiResponse<?> responseBuilder;
    private final PetDailyPlanService petDailyPlanService;
    private final PetAccessGuard petAccessGuard;
    private final UserRepository userRepository;

    @PostMapping("/nutrition/generate/daily-plan")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<PetDailyPlanModel>>> generatePetDailyPlan(
            @RequestBody @Valid List<PetDailyPlanDto> petDailyPlanDto,
            @RequestParam String petUuid,
            @RequestParam(required = false) String userUuid,
            @RequestParam String nutritionPlanUuid) {
        User caller = currentUser();
        petAccessGuard.requirePetAccess(caller, petUuid);
        // Ignore client-supplied userUuid — bind to authenticated caller.
        List<PetDailyPlanModel> response = petDailyPlanService.createReplaceCurrentMonthPlan(
                caller.getUuid(), petUuid, nutritionPlanUuid, petDailyPlanDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping("/nutrition/pets/{petUuid}/daily-plan")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<PetDailyPlanModel>>> activeDailyPlan(
            @PathVariable String petUuid) {
        petAccessGuard.requirePetAccess(currentUser(), petUuid);
        return responseBuilder.buildSuccessResponse(
                petDailyPlanService.getPetsDailyPlanActivePlan(petUuid),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
