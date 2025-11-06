package com.kittyp.ai.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.ai.dto.NutritionPlanFilter;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.service.NutritionPlanService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class NutritionPlanController {
    
    private final ApiResponse<?> responseBuilder;
    private final NutritionPlanService nutritionPlanService;

    @PostMapping("/ai/nutrition/plans/filter")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PaginationModel<NutritionPlanModel>>> filterNutritionPlan(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection,
            @RequestParam(required =  false) String petUuid,
            @RequestParam(required =  false) String uuid,
            @RequestParam(required =  false) String searchText,
            @RequestParam(required =  false) String userUuid,
            @RequestParam(required =  false) Boolean isActive,
            @RequestParam(required =  false) List<String> tags){

        NutritionPlanFilter nutritionPlanFilter = NutritionPlanFilter.builder()
                .petUuid(petUuid)
                .uuid(uuid)
                .userUuid(userUuid)
                .isActive(isActive)
                .searchText(searchText)
                .tags(tags)
                .sortBy(sortBy)
                .sortDirection(Sort.Direction.fromString(sortDirection))
                .build();
                
        PaginationModel<NutritionPlanModel> response = nutritionPlanService.getNutritionPlansForPet(nutritionPlanFilter, page, size);

        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
