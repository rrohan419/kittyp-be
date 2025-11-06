package com.kittyp.ai.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.ai.dao.NutritionPlanDao;
import com.kittyp.ai.dto.EnvironmentDataDto;
import com.kittyp.ai.dto.NutritionPlanFilter;
import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.model.NutritionRecommendationResponse;
import com.kittyp.ai.model.NutritionRecommendationResponse.DailyFeedingPlan;
import com.kittyp.ai.model.NutritionRecommendationResponse.Environment;
import com.kittyp.ai.model.NutritionRecommendationResponse.EnvironmentalImpact;
import com.kittyp.ai.model.NutritionRecommendationResponse.PetProfileSummary;
import com.kittyp.ai.model.NutritionRecommendationResponse.RecommendedProduct;
import com.kittyp.ai.model.NutritionRecommendationResponse.SpecialConsideration;
import com.kittyp.ai.repository.NutritionPlanRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionPlanServiceImpl implements NutritionPlanService {

    private final NutritionPlanRepository nutritionPlanRepository;
    private final NutritionPlanDao nutritionPlanDao;
    private final Mapper mapper;

    @Async("taskExecutor")
    @Transactional
    @Override
    public CompletableFuture<NutritionPlan> saveNutritionPlanAsync(
            String petUuid,
            String userUuid,
            NutritionRecommendationResponse nutritionResponse,
            EnvironmentDataDto environmentData,
            String planName) {

        log.info("Saving nutrition plan asynchronously for pet: {} and user: {}", petUuid, userUuid);

        try {

            // Create nutrition plan entity
            NutritionPlan nutritionPlan = NutritionPlan.builder()
                    .uuid(UUID.randomUUID().toString())
                    .petUuid(petUuid)
                    .userUuid(userUuid)
                    .planName(planName != null ? planName : generateDefaultPlanName())
                    .petProfileSummary(mapper.convertObjectToJson(nutritionResponse.getPetProfileSummary()))
                    .environmentalImpact(mapper.convertObjectToJson(nutritionResponse.getEnvironmentalImpact()))
                    .dailyFeedingPlan(mapper.convertObjectToJson(nutritionResponse.getDailyFeedingPlan()))
                    .specialConsiderations(mapper.convertObjectToJson(nutritionResponse.getSpecialConsiderations()))
                    .recommendedProducts(mapper.convertObjectToJson(nutritionResponse.getRecommendedProducts()))
                    .longTermWellnessTips(mapper.convertObjectToJson(nutritionResponse.getLongTermWellnessTips()))
                    .environment(mapper.convertObjectToJson(environmentData))
                    .generationTimestamp(LocalDateTime.now())
                    .build();

            // Save to database
            NutritionPlan savedPlan = nutritionPlanDao.saveNutritionPlan(nutritionPlan);

            log.info("Successfully saved nutrition plan with UUID: {} for pet: {}",
                    savedPlan.getUuid(), petUuid);

            return CompletableFuture.completedFuture(savedPlan);

        } catch (Exception e) {
            log.error("Error saving nutrition plan for pet: {} and user: {}", petUuid, userUuid, e);
            CompletableFuture<NutritionPlan> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    @Override
    public PaginationModel<NutritionPlanModel> getNutritionPlansForPet(NutritionPlanFilter nutritionPlanFilter,
            int page, int size) {

        Sort sort = Sort.by(nutritionPlanFilter.getSortDirection(), nutritionPlanFilter.getSortBy());
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<NutritionPlan> specification = NutritionPlanSpecification.filters(nutritionPlanFilter);
        Page<NutritionPlan> nutritionPlanPage = nutritionPlanDao.findAllByFilter(specification, pageable);

        return mapToPaginationModel(nutritionPlanPage);
    }

    /**
     * Helper method to map Page<NutritionPlan> to
     * PaginationModel<NutritionPlanModel>
     * 
     * @param nutritionPlanPage
     * @return
     */
    private PaginationModel<NutritionPlanModel> mapToPaginationModel(Page<NutritionPlan> nutritionPlanPage) {

        List<NutritionPlanModel> nutritionPlanModels = nutritionPlanPage.getContent().stream().map(nutritionPlan -> {
            NutritionPlanModel nutritionPlanModel = mapper.convert(nutritionPlan, NutritionPlanModel.class);
            nutritionPlanModel.setNutritionRecommendationResponse(mapData(nutritionPlan));
            return nutritionPlanModel;
        }).toList();

        return PaginationModel.<NutritionPlanModel>builder()
                .models(nutritionPlanModels)
                .pageNumber(nutritionPlanPage.getNumber())
                .pageSize(nutritionPlanPage.getSize())
                .totalElements(nutritionPlanPage.getTotalElements())
                .totalPages(nutritionPlanPage.getTotalPages())
                .isFirst(nutritionPlanPage.isFirst())
                .isLast(nutritionPlanPage.isLast())
                .build();
    }

    @Transactional
    @Override
    public NutritionPlanModel setAsActivePlan(String planUuid, String petUuid) {
        // First, deactivate all current active plans for this pet
        List<NutritionPlan> currentActivePlans = nutritionPlanRepository.findByPetUuidAndIsActiveTrue(petUuid);
        currentActivePlans.forEach(plan -> plan.setIsActivePlan(false));
        nutritionPlanRepository.saveAll(currentActivePlans);

        // Then activate the selected plan
        Optional<NutritionPlan> planToActivate = nutritionPlanRepository.findByUuid(planUuid);
        if (planToActivate.isPresent()) {
            NutritionPlan plan = planToActivate.get();
            plan.setIsActivePlan(true);
            plan = nutritionPlanRepository.save(plan);
            NutritionPlanModel nutritionPlanModel = mapper.convert(plan, NutritionPlanModel.class);

            nutritionPlanModel.setNutritionRecommendationResponse(mapData(plan));
            return nutritionPlanModel;
        } else {
            throw new CustomException("Nutrition plan not found with UUID: " + planUuid);
        }
    }

    private NutritionRecommendationResponse mapData(NutritionPlan plan) {
        NutritionRecommendationResponse nutritionRecommendationResponse = new NutritionRecommendationResponse();

        try {
            // Daily Feeding Plan
            if (isValidJson(plan.getDailyFeedingPlan())) {
                nutritionRecommendationResponse.setDailyFeedingPlan(
                        mapper.convertJsonToObejct(new JSONObject(plan.getDailyFeedingPlan()), DailyFeedingPlan.class));
            }

            // Pet Profile Summary
            if (isValidJson(plan.getPetProfileSummary())) {
                nutritionRecommendationResponse.setPetProfileSummary(
                        mapper.convertJsonToObejct(new JSONObject(plan.getPetProfileSummary()),
                                PetProfileSummary.class));
            }

            // Environmental Impact
            if (isValidJson(plan.getEnvironmentalImpact())) {
                nutritionRecommendationResponse.setEnvironmentalImpact(
                        mapper.convertJsonToObejct(new JSONObject(plan.getEnvironmentalImpact()),
                                EnvironmentalImpact.class));
            }

            // Special Considerations (list)
            if (isNonEmpty(plan.getSpecialConsiderations())) {
                nutritionRecommendationResponse.setSpecialConsiderations(
                        mapper.convertJsonToList(plan.getSpecialConsiderations(), SpecialConsideration.class));
            }

            // Recommended Products (list)
            if (isNonEmpty(plan.getRecommendedProducts())) {
                nutritionRecommendationResponse.setRecommendedProducts(
                        mapper.convertJsonToList(plan.getRecommendedProducts(), RecommendedProduct.class));
            }

            // Long Term Wellness Tips (list)
            if (isNonEmpty(plan.getLongTermWellnessTips())) {
                nutritionRecommendationResponse.setLongTermWellnessTips(
                        mapper.convertJsonToList(plan.getLongTermWellnessTips(), String.class));
            }

            // Environment
            if (isValidJson(plan.getEnvironment())) {
                nutritionRecommendationResponse.setEnvironment(
                        mapper.convertJsonToObejct(new JSONObject(plan.getEnvironment()), Environment.class));
            }

        } catch (Exception e) {
            log.error("Error mapping NutritionPlan to NutritionRecommendationResponse for plan UUID: {}",
                    plan.getUuid(), e);
        }

        return nutritionRecommendationResponse;
    }

    private boolean isValidJson(String json) {
        if (json == null || json.isBlank())
            return false;
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) || (json.startsWith("[") && json.endsWith("]"));
    }

    private boolean isNonEmpty(String json) {
        return json != null && !json.isBlank() && !json.equalsIgnoreCase("null") && json.length() > 2;
    }

    @Transactional
    @Override
    public void deletePlan(String planUuid) {
        Optional<NutritionPlan> plan = nutritionPlanRepository.findByUuid(planUuid);
        if (plan.isPresent()) {
            NutritionPlan nutritionPlan = plan.get();
            nutritionPlan.setIsActive(false); // Soft delete
            nutritionPlanRepository.save(nutritionPlan);
            log.info("Soft deleted nutrition plan with UUID: {}", planUuid);
        } else {
            throw new CustomException("Nutrition plan not found with UUID: " + planUuid);
        }
    }

    @Transactional
    @Override
    public NutritionPlan updatePlanNotes(String planUuid, String notes) {
        Optional<NutritionPlan> plan = nutritionPlanRepository.findByUuid(planUuid);
        if (plan.isPresent()) {
            NutritionPlan nutritionPlan = plan.get();
            nutritionPlan.setNotes(notes);
            return nutritionPlanRepository.save(nutritionPlan);
        } else {
            throw new CustomException("Nutrition plan not found with UUID: " + planUuid);
        }
    }

    private String generateDefaultPlanName() {
        return "Nutrition Plan - " + LocalDate.now().toString();
    }
}
