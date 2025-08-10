package com.kittyp.ai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.ai.dto.NutritionistRecommendationRequest;
import com.kittyp.ai.model.NutritionRecommendationResponse;
import com.kittyp.ai.service.AiNutritionRecommendationService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AiController {

    private final ApiResponse<?> responseBuilder;
    private final AiNutritionRecommendationService aiNutritionRecommendationService;

    @PostMapping("/ai/nutrition/generate")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<NutritionRecommendationResponse>> generateNutritionAIData(
            @RequestBody @Valid NutritionistRecommendationRequest nutritionistRecommendationRequest,
            HttpServletRequest httpServletRequest) {
        NutritionRecommendationResponse response = aiNutritionRecommendationService
                .getNutritionRecommendationRequest(nutritionistRecommendationRequest, httpServletRequest);

        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
