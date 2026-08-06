package com.kittyp.ai.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.ai.dto.NutritionistRecommendationRequest;
import com.kittyp.ai.model.NutritionRecommendationResponse;
import com.kittyp.ai.model.TipOfTheDayModel;
import com.kittyp.ai.service.AiNutritionRecommendationService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AiController {

    private final ApiResponse<?> responseBuilder;
    private final AiNutritionRecommendationService aiNutritionRecommendationService;
    private final UserDao userDao;

    @PostMapping("/ai/nutrition/generate")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<NutritionRecommendationResponse>> generateNutritionAIData(
            @RequestBody @Valid NutritionistRecommendationRequest nutritionistRecommendationRequest,
            HttpServletRequest httpServletRequest) {
        NutritionRecommendationResponse response = aiNutritionRecommendationService
                .getNutritionRecommendationRequest(nutritionistRecommendationRequest, httpServletRequest);

        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    /** Loop 2 alias: POST /api/v1/ai/nutrition-plan */
    @PostMapping("/ai/nutrition-plan")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<NutritionRecommendationResponse>> generateNutritionPlan(
            @RequestBody @Valid NutritionistRecommendationRequest nutritionistRecommendationRequest,
            HttpServletRequest httpServletRequest) {
        return generateNutritionAIData(nutritionistRecommendationRequest, httpServletRequest);
    }

    @GetMapping(ApiUrl.AI_TIP_OF_THE_DAY)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<TipOfTheDayModel>> tipOfTheDay(@RequestParam String petUuid) {
        assertPetOwner(petUuid);
        LocalDate today = LocalDate.now();
        List<String> tips = List.of(
                "Keep fresh water available and wash the bowl daily.",
                "A short daily play session supports a healthy weight and reduces stress.",
                "Use treats sparingly and include their calories in your pet's daily food allowance.",
                "Check your pet's coat, ears, and paws during regular grooming.",
                "Sudden changes in appetite, thirst, or energy are worth discussing with a veterinarian.");
        int index = Math.floorMod((petUuid + today).hashCode(), tips.size());
        return responseBuilder.buildSuccessResponse(new TipOfTheDayModel(tips.get(index), today),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private void assertPetOwner(String petUuid) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userDao.userByEmail(email);
        User petOwner = userDao.userByPetUuid(petUuid);
        if (!authenticatedUser.getUuid().equals(petOwner.getUuid())) {
            throw new CustomException("You are not authorized to access this pet", HttpStatus.FORBIDDEN);
        }
    }
}
