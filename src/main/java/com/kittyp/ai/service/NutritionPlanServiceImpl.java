package com.kittyp.ai.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.ai.dao.NutritionPlanDao;
import com.kittyp.ai.dto.EnvironmentDataDto;
import com.kittyp.ai.dto.NutritionPlanFilter;
import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.enums.NutritionPlanStatus;
import com.kittyp.ai.model.NutritionPlanModel;
import com.kittyp.ai.model.NutritionRecommendationResponse;
import com.kittyp.ai.model.NutritionRecommendationResponse.DailyFeedingPlan;
import com.kittyp.ai.model.NutritionRecommendationResponse.Environment;
import com.kittyp.ai.model.NutritionRecommendationResponse.EnvironmentalImpact;
import com.kittyp.ai.model.NutritionRecommendationResponse.PetProfileSummary;
import com.kittyp.ai.model.NutritionRecommendationResponse.RecommendedProduct;
import com.kittyp.ai.model.NutritionRecommendationResponse.SpecialConsideration;
import com.kittyp.ai.repository.NutritionPlanRepository;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.nutrition.entity.PetDailyPlan;
import com.kittyp.nutrition.enums.ItemType;
import com.kittyp.nutrition.service.PetDailyPlanService;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.user.service.PetAccessGuard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionPlanServiceImpl implements NutritionPlanService {

    private final NutritionPlanRepository nutritionPlanRepository;
    private final NutritionPlanDao nutritionPlanDao;
    private final Mapper mapper;
    private final PetDailyPlanService petDailyPlanService;
    private final PetAccessGuard petAccessGuard;
    private final UserRepository userRepository;
    private final PetsRepository petsRepository;

    @Override
    @Transactional
    public CompletableFuture<NutritionPlan> saveNutritionPlanAsync(
            String petUuid,
            String userUuid,
            NutritionRecommendationResponse nutritionResponse,
            EnvironmentDataDto environmentData,
            String planName) {

        log.info("Saving nutrition plan for pet: {} and user: {}", petUuid, userUuid);

        try {
            LocalDate today = LocalDate.now();
            int month = today.getMonthValue();
            int year = today.getYear();

            nutritionPlanDao.deactivatePlansForMonth(petUuid, month, year);

            String parentUuid = resolveParentUserUuid(petUuid);
            boolean doctorCaller = isDoctorCaller(userUuid);

            NutritionPlan nutritionPlan = NutritionPlan.builder()
                    .uuid(nutritionResponse.getUuid())
                    .petUuid(petUuid)
                    .userUuid(doctorCaller && parentUuid != null ? parentUuid : userUuid)
                    .parentUserUuid(parentUuid)
                    .doctorUserUuid(doctorCaller ? userUuid : null)
                    .status(NutritionPlanStatus.DRAFT)
                    .planName(planName != null ? planName : generateDefaultPlanName())
                    .petProfileSummary(mapper.convertObjectToJson(nutritionResponse.getPetProfileSummary()))
                    .environmentalImpact(mapper.convertObjectToJson(nutritionResponse.getEnvironmentalImpact()))
                    .dailyFeedingPlan(mapper.convertObjectToJson(nutritionResponse.getDailyFeedingPlan()))
                    .specialConsiderations(mapper.convertObjectToJson(nutritionResponse.getSpecialConsiderations()))
                    .recommendedProducts(mapper.convertObjectToJson(nutritionResponse.getRecommendedProducts()))
                    .longTermWellnessTips(mapper.convertObjectToJson(nutritionResponse.getLongTermWellnessTips()))
                    .environment(mapper.convertObjectToJson(environmentData))
                    .generationTimestamp(LocalDateTime.now())
                    .isActivePlan(Boolean.TRUE)
                    .planMonth(month)
                    .planYear(year)
                    .build();

            NutritionPlan savedPlan = nutritionPlanDao.saveNutritionPlan(nutritionPlan);

            log.info("Successfully saved nutrition plan with UUID: {} for pet: {}",
                    savedPlan.getUuid(), petUuid);

            return CompletableFuture.completedFuture(savedPlan);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error saving nutrition plan for pet: {} and user: {}", petUuid, userUuid, e);
            throw new CustomException("Error saving nutrition plan");
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

    @Transactional
    @Override
    public NutritionPlan updatePlanContent(String planUuid, String doctorUserUuid,
            NutritionRecommendationResponse nutritionResponse, EnvironmentDataDto environmentData) {
        NutritionPlan plan = getPlan(planUuid);
        if (plan.getStatus() == NutritionPlanStatus.SENT) {
            throw new CustomException("Sent nutrition plans cannot be edited");
        }
        assertDoctorClinicalAccess(doctorUserUuid, plan.getPetUuid());
        if (nutritionResponse != null) {
            if (nutritionResponse.getPetProfileSummary() != null) {
                plan.setPetProfileSummary(mapper.convertObjectToJson(nutritionResponse.getPetProfileSummary()));
            }
            if (nutritionResponse.getEnvironmentalImpact() != null) {
                plan.setEnvironmentalImpact(mapper.convertObjectToJson(nutritionResponse.getEnvironmentalImpact()));
            }
            if (nutritionResponse.getDailyFeedingPlan() != null) {
                plan.setDailyFeedingPlan(mapper.convertObjectToJson(nutritionResponse.getDailyFeedingPlan()));
            }
            if (nutritionResponse.getSpecialConsiderations() != null) {
                plan.setSpecialConsiderations(mapper.convertObjectToJson(nutritionResponse.getSpecialConsiderations()));
            }
            if (nutritionResponse.getRecommendedProducts() != null) {
                plan.setRecommendedProducts(mapper.convertObjectToJson(nutritionResponse.getRecommendedProducts()));
            }
            if (nutritionResponse.getLongTermWellnessTips() != null) {
                plan.setLongTermWellnessTips(mapper.convertObjectToJson(nutritionResponse.getLongTermWellnessTips()));
            }
        }
        if (environmentData != null) {
            plan.setEnvironment(mapper.convertObjectToJson(environmentData));
        } else if (nutritionResponse != null && nutritionResponse.getEnvironment() != null) {
            plan.setEnvironment(mapper.convertObjectToJson(nutritionResponse.getEnvironment()));
        }
        plan.setDoctorUserUuid(doctorUserUuid);
        return nutritionPlanRepository.save(plan);
    }

    @Transactional
    @Override
    public NutritionPlan approvePlan(String planUuid, String doctorUserUuid) {
        NutritionPlan plan = getPlan(planUuid);
        assertDoctorClinicalAccess(doctorUserUuid, plan.getPetUuid());
        plan.setDoctorUserUuid(doctorUserUuid);
        plan.setStatus(NutritionPlanStatus.APPROVED);
        plan.setApprovedAt(LocalDateTime.now());
        return nutritionPlanRepository.save(plan);
    }

    @Transactional
    @Override
    public NutritionPlan sendPlan(String planUuid, String doctorUserUuid) {
        NutritionPlan plan = getPlan(planUuid);
        assertDoctorClinicalAccess(doctorUserUuid, plan.getPetUuid());
        plan.setDoctorUserUuid(doctorUserUuid);
        if (plan.getApprovedAt() == null) {
            plan.setApprovedAt(LocalDateTime.now());
        }
        String parentUuid = plan.getParentUserUuid();
        if (parentUuid == null || parentUuid.isBlank()) {
            parentUuid = resolveParentUserUuid(plan.getPetUuid());
        }
        if (parentUuid == null || parentUuid.isBlank()) {
            parentUuid = plan.getUserUuid();
        }
        plan.setParentUserUuid(parentUuid);
        plan.setStatus(NutritionPlanStatus.SENT);
        plan.setSentAt(LocalDateTime.now());
        plan.setIsActivePlan(true);
        NutritionPlan saved = nutritionPlanRepository.save(plan);

        // Materialize a 30-day interactive schedule for parent + doctor tracking
        try {
            NutritionRecommendationResponse recommendation = mapData(saved);
            if (recommendation != null && recommendation.getDailyFeedingPlan() != null) {
                List<PetDailyPlan> templates = convertToDailyPlanTemplates(recommendation.getDailyFeedingPlan());
                if (!templates.isEmpty()) {
                    petDailyPlanService.createOrReplaceNextDaysPlan(
                            parentUuid,
                            saved.getPetUuid(),
                            saved.getUuid(),
                            templates,
                            30);
                }
            }
        } catch (Exception e) {
            log.error("Failed to materialize 30-day daily plans for nutrition plan {}", planUuid, e);
        }

        return saved;
    }

    @Override
    public NutritionPlan getActivePlanForParent(String petUuid, String parentUserUuid) {
        return nutritionPlanRepository
                .findFirstByPetUuidAndParentUserUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
                        petUuid, parentUserUuid, NutritionPlanStatus.SENT)
                .or(() -> nutritionPlanRepository.findFirstByPetUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
                        petUuid, NutritionPlanStatus.SENT))
                .orElseThrow(() -> new CustomException("No sent nutrition plan found for this pet"));
    }

    @Override
    public NutritionPlan getActiveSentPlanForPet(String petUuid) {
        return nutritionPlanRepository
                .findFirstByPetUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(petUuid, NutritionPlanStatus.SENT)
                .orElseThrow(() -> new CustomException("No sent nutrition plan found for this pet"));
    }

    private NutritionPlan getPlan(String planUuid) {
        return nutritionPlanRepository.findByUuid(planUuid)
                .orElseThrow(() -> new CustomException("Nutrition plan not found with UUID: " + planUuid));
    }

    private void assertDoctorClinicalAccess(String doctorUserUuid, String petUuid) {
        User doctor = userRepository.findByUuid(doctorUserUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", doctorUserUuid));
        petAccessGuard.requireClinicalAccess(doctor, petUuid);
    }

    private boolean isDoctorCaller(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .map(petAccessGuard::isDoctorLike)
                .orElse(false);
    }

    private String resolveParentUserUuid(String petUuid) {
        Pet pet = petsRepository.findOptionalByUuid(petUuid).orElse(null);
        if (pet != null) {
            if (pet.getParentUserUuid() != null && !pet.getParentUserUuid().isBlank()) {
                return pet.getParentUserUuid();
            }
            ClinicPetOwner clinicOwner = pet.getClinicOwner();
            if (clinicOwner != null && clinicOwner.getLinkedUser() != null) {
                return clinicOwner.getLinkedUser().getUuid();
            }
        }
        return userRepository.findByPets_Uuid(petUuid)
                .map(User::getUuid)
                .orElse(null);
    }

    private String generateDefaultPlanName() {
        return "Nutrition Plan - " + LocalDate.now().toString();
    }

    /**
     * Converts DailyFeedingPlan to List<PetDailyPlan> templates.
     * These templates will be used to create daily plans for the current month.
     * 
     * @param dailyFeedingPlan The daily feeding plan from nutrition recommendation
     * @return List of PetDailyPlan templates
     */
    private List<PetDailyPlan> convertToDailyPlanTemplates(DailyFeedingPlan dailyFeedingPlan) {
        List<PetDailyPlan> templates = new ArrayList<>();

        if (dailyFeedingPlan == null) {
            return templates;
        }

        // Convert meals to FOOD items
        if (dailyFeedingPlan.getMeals() != null) {
            for (DailyFeedingPlan.Meal meal : dailyFeedingPlan.getMeals()) {
                try {
                    LocalTime time = parseTime(meal.getTime());
                    PetDailyPlan plan = PetDailyPlan.builder()
                            .itemType(ItemType.FOOD)
                            .itemName(meal.getFoodType() != null ? meal.getFoodType() : "Food")
                            .time(time)
                            .quantityInGrams(meal.getPortionSizeGrams() > 0
                                    ? (double) meal.getPortionSizeGrams()
                                    : null)
                            .notes(meal.getNotes())
                            .build();
                    templates.add(plan);
                } catch (Exception e) {
                    log.warn("Error converting meal to daily plan template: {}", meal, e);
                }
            }
        }

        // Convert supplements to SUPPLEMENT items
        if (dailyFeedingPlan.getSupplements() != null) {
            for (DailyFeedingPlan.Supplement supplement : dailyFeedingPlan.getSupplements()) {
                try {
                    // For supplements, use a default morning time if not specified
                    // In a real scenario, you might want to parse this from the dosage or have it
                    // in the response
                    LocalTime time = LocalTime.of(8, 0); // Default to 8:00 AM

                    // Combine purpose and dosage in notes if available
                    String notes = "";
                    if (supplement.getPurpose() != null && !supplement.getPurpose().isEmpty()) {
                        notes = supplement.getPurpose();
                    }
                    if (supplement.getDosage() != null && !supplement.getDosage().isEmpty()) {
                        if (!notes.isEmpty()) {
                            notes += " - " + supplement.getDosage();
                        } else {
                            notes = supplement.getDosage();
                        }
                    }

                    PetDailyPlan plan = PetDailyPlan.builder()
                            .itemType(ItemType.SUPPLEMENT)
                            .itemName(supplement.getName() != null ? supplement.getName() : "Supplement")
                            .time(time)
                            .quantityInGrams(null) // Supplements typically don't have grams, dosage is in notes
                            .notes(notes.isEmpty() ? null : notes)
                            .build();
                    templates.add(plan);
                } catch (Exception e) {
                    log.warn("Error converting supplement to daily plan template: {}", supplement, e);
                }
            }
        }

        return templates;
    }

    /**
     * Parses time string to LocalTime.
     * Supports formats like "HH:mm", "HH:mm:ss", "h:mm a" (12-hour format), etc.
     * 
     * @param timeStr Time string to parse
     * @return LocalTime object
     * @throws IllegalArgumentException if time cannot be parsed
     */
//     private LocalTime parseTime(String timeStr) {
//     if (timeStr == null || timeStr.trim().isEmpty()) {
//         throw new IllegalArgumentException("Time string cannot be null or empty");
//     }

//     String trimmed = timeStr.trim();

//     try {
//         // 24-hour format (HH:mm)
//         if (trimmed.matches("\\d{1,2}:\\d{2}")) {
//             return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm"));
//         }

//         // 24-hour with seconds (HH:mm:ss)
//         if (trimmed.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
//             return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm:ss"));
//         }

//         // 12-hour format (h:mm a or h:mm AM/PM)
//         if (trimmed.matches("(?i)\\d{1,2}:\\d{2}\\s*(AM|PM)")) {
//             DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
//             return LocalTime.parse(trimmed.toUpperCase(Locale.ENGLISH), formatter);
//         }

//         // ISO or fallback
//         return LocalTime.parse(trimmed);
//     } catch (DateTimeParseException e) {
//         log.warn("Failed to parse time string '{}', defaulting to 08:00 AM", trimmed, e);
//         return LocalTime.of(8, 0);
//     }
// }

private LocalTime parseTime(String timeStr) {
    if (timeStr == null || timeStr.trim().isEmpty()) {
        throw new IllegalArgumentException("Time string cannot be null or empty");
    }

    String trimmed = timeStr.trim();

    // Extract time like "7:00 AM", "18:30", "18:30:10"
    Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2}(:\\d{2})?\\s*(?i)(AM|PM)?)");
    Matcher matcher = pattern.matcher(trimmed);

    if (matcher.find()) {
        trimmed = matcher.group().trim();
    }

    try {

        if (trimmed.matches("\\d{1,2}:\\d{2}")) {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm"));
        }

        if (trimmed.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm:ss"));
        }

        if (trimmed.matches("(?i)\\d{1,2}:\\d{2}\\s*(AM|PM)")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
            return LocalTime.parse(trimmed.toUpperCase(Locale.ENGLISH), formatter);
        }

        return LocalTime.parse(trimmed);

    } catch (DateTimeParseException e) {
        log.warn("Failed to parse time string '{}', defaulting to 08:00 AM", trimmed, e);
        return LocalTime.of(8, 0);
    }
}
}
