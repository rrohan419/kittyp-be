package com.kittyp.ai.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
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

import com.kittyp.ai.dto.NutritionPlanFilter;
import com.kittyp.ai.dto.SaveNutritionPlanDto;
import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.service.NutritionPlanService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.user.service.PetAccessGuard;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class NutritionPlanController {

    private final ApiResponse<?> responseBuilder;
    private final NutritionPlanService nutritionPlanService;
    private final UserRepository userRepository;
    private final PetAccessGuard petAccessGuard;

    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    @PostMapping("/ai/nutrition/plan/save")
    public ResponseEntity<SuccessResponse<String>> saveNutritionPlanAsync(
            @RequestBody SaveNutritionPlanDto saveNutritionPlanDto) {
        User caller = currentUser();
        petAccessGuard.requirePetAccess(caller, saveNutritionPlanDto.getPetUuid());
        // Always bind ownership to the authenticated caller — ignore client userUuid.
        nutritionPlanService.saveNutritionPlanAsync(saveNutritionPlanDto.getPetUuid(),
                caller.getUuid(), saveNutritionPlanDto.getRecommendationResponse(),
                saveNutritionPlanDto.getEnvironmentDataDto(),
                saveNutritionPlanDto.getPetName() + "'s Nutrition Plan" + "-" + LocalDate.now());

        return responseBuilder.buildSuccessResponse(ResponseMessage.PET_PLAN_SAVED_SUCCESSFULLY,
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping("/ai/nutrition/plans/filter")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PaginationModel<NutritionPlanModel>>> filterNutritionPlan(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String petUuid,
            @RequestParam(required = false) String uuid,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String userUuid,
            @RequestParam(required = false) String doctorUserUuid,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> tags) {

        User caller = currentUser();
        String scopedUserUuid = userUuid;
        String scopedDoctorUuid = doctorUserUuid;

        if (!petAccessGuard.isAdmin(caller)) {
            if (petUuid != null && !petUuid.isBlank()) {
                petAccessGuard.requirePetAccess(caller, petUuid);
            }

            if (petAccessGuard.isDoctorLike(caller)) {
                // Doctors may only query their own doctor scope or a pet they can access.
                if (scopedDoctorUuid != null && !scopedDoctorUuid.equals(caller.getUuid())) {
                    scopedDoctorUuid = caller.getUuid();
                }
                if (scopedUserUuid != null && !scopedUserUuid.equals(caller.getUuid())
                        && (petUuid == null || petUuid.isBlank())) {
                    throw new CustomException("You are not authorized to list plans for that user",
                            HttpStatus.FORBIDDEN);
                }
                if ((petUuid == null || petUuid.isBlank())
                        && (scopedUserUuid == null || scopedUserUuid.isBlank())
                        && (scopedDoctorUuid == null || scopedDoctorUuid.isBlank())) {
                    scopedDoctorUuid = caller.getUuid();
                }
            } else {
                // Pet parents: always scoped to self; never honor foreign userUuid.
                scopedUserUuid = caller.getUuid();
                scopedDoctorUuid = null;
            }
        }

        NutritionPlanFilter nutritionPlanFilter = NutritionPlanFilter.builder()
                .petUuid(petUuid)
                .uuid(uuid)
                .userUuid(scopedUserUuid)
                .doctorUserUuid(scopedDoctorUuid)
                .isActive(isActive)
                .status(status)
                .searchText(searchText)
                .tags(tags)
                .sortBy(sortBy)
                .sortDirection(Sort.Direction.fromString(sortDirection))
                .build();

        PaginationModel<NutritionPlanModel> response = nutritionPlanService.getNutritionPlansForPet(nutritionPlanFilter,
                page, size);

        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.NUTRITION_PLAN_APPROVE)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<NutritionPlan>> approvePlan(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(
                nutritionPlanService.approvePlan(uuid, currentUser().getUuid()),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.NUTRITION_PLAN_SEND)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<NutritionPlan>> sendPlan(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(
                nutritionPlanService.sendPlan(uuid, currentUser().getUuid()),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.NUTRITION_PLAN_ACTIVE)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<NutritionPlan>> activePlan(@RequestParam String petUuid) {
        User caller = currentUser();
        petAccessGuard.requirePetAccess(caller, petUuid);
        return responseBuilder.buildSuccessResponse(
                nutritionPlanService.getActivePlanForParent(petUuid, caller.getUuid()),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
