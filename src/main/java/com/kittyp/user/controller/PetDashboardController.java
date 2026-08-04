package com.kittyp.user.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.ai.dao.NutritionPlanDao;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.nutrition.dao.PetFeedingLogDao;
import com.kittyp.nutrition.enums.FeedingStatus;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.PetWeightLogRequest;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.PetWeightLog;
import com.kittyp.user.entity.User;
import com.kittyp.user.models.PetDashboardModel;
import com.kittyp.user.models.PetModel;
import com.kittyp.user.models.PetWeightLogModel;
import com.kittyp.user.repository.PetWeightLogRepository;
import com.kittyp.vaccine.dao.PetVaccineScheduleDao;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PetDashboardController {

    private static final List<String> DAILY_TIPS = List.of(
            "Keep fresh water available and wash the bowl daily.",
            "A short daily play session supports a healthy weight and reduces stress.",
            "Use treats sparingly and include their calories in your pet's daily food allowance.",
            "Check your pet's coat, ears, and paws during regular grooming.",
            "Sudden changes in appetite, thirst, or energy are worth discussing with a veterinarian.");

    private final ApiResponse<?> responseBuilder;
    private final UserDao userDao;
    private final PetDao petDao;
    private final PetWeightLogRepository petWeightLogRepository;
    private final PetVaccineScheduleDao petVaccineScheduleDao;
    private final NutritionPlanDao nutritionPlanDao;
    private final PetFeedingLogDao petFeedingLogDao;

    @PostMapping(ApiUrl.PET_WEIGHT)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetWeightLogModel>> logWeight(
            @PathVariable String uuid,
            @RequestBody @Valid PetWeightLogRequest request) {
        Pet pet = ownedPet(uuid);
        LocalDateTime recordedAt = request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt();
        Optional<PetWeightLog> latest = petWeightLogRepository.findFirstByPet_UuidOrderByRecordedAtDesc(uuid);
        PetWeightLog saved = petWeightLogRepository.save(PetWeightLog.builder()
                .pet(pet)
                .weight(request.weight())
                .recordedAt(recordedAt)
                .note(request.note())
                .build());
        if (latest.isEmpty() || !recordedAt.isBefore(latest.get().getRecordedAt())) {
            pet.setWeight(String.valueOf(request.weight()));
            petDao.savePets(pet);
        }
        return responseBuilder.buildSuccessResponse(toWeightModel(saved), ResponseMessage.SUCCESS, HttpStatus.CREATED);
    }

    @GetMapping(ApiUrl.PET_WEIGHT_HISTORY)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<PetWeightLogModel>>> weightHistory(@PathVariable String uuid) {
        ownedPet(uuid);
        List<PetWeightLogModel> history = petWeightLogRepository.findByPet_UuidOrderByRecordedAtDesc(uuid).stream()
                .map(this::toWeightModel)
                .toList();
        return responseBuilder.buildSuccessResponse(history, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.PET_DASHBOARD)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetDashboardModel>> dashboard(@PathVariable String uuid) {
        Pet pet = ownedPet(uuid);
        LocalDate today = LocalDate.now();
        PetDashboardModel response = new PetDashboardModel(
                toPetModel(pet),
                petWeightLogRepository.findFirstByPet_UuidOrderByRecordedAtDesc(uuid).map(this::toWeightModel).orElse(null),
                vaccineDues(uuid, today),
                activeNutritionPlan(uuid),
                completedFeedingsToday(uuid, today),
                tipFor(uuid, today));
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private Pet ownedPet(String petUuid) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userDao.userByEmail(email);
        User petOwner = userDao.userByPetUuid(petUuid);
        if (!authenticatedUser.getUuid().equals(petOwner.getUuid())) {
            throw new CustomException("You are not authorized to access this pet", HttpStatus.FORBIDDEN);
        }
        Pet pet = petDao.petByUuid(petUuid);
        if (pet == null) {
            throw new CustomException("Pet not found by uuid: " + petUuid, HttpStatus.NOT_FOUND);
        }
        return pet;
    }

    private List<PetDashboardModel.VaccineDueModel> vaccineDues(String petUuid, LocalDate today) {
        return petVaccineScheduleDao.findByPetUuid(petUuid).stream()
                .filter(schedule -> !Boolean.TRUE.equals(schedule.getCompleted()))
                .filter(schedule -> schedule.getDueDate() != null && !schedule.getDueDate().isAfter(today))
                .map(schedule -> new PetDashboardModel.VaccineDueModel(schedule.getVaccine().getName(), schedule.getDueDate()))
                .toList();
    }

    private PetDashboardModel.ActiveNutritionPlanModel activeNutritionPlan(String petUuid) {
        return nutritionPlanDao.getNutritionPlansByPetUuidAndIsActiveTrue(petUuid)
                .map(plan -> new PetDashboardModel.ActiveNutritionPlanModel(
                        plan.getUuid(), plan.getPlanName(), plan.getGenerationTimestamp()))
                .orElse(null);
    }

    private int completedFeedingsToday(String petUuid, LocalDate today) {
        return (int) petFeedingLogDao.findForDay(petUuid, today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .filter(log -> log.getStatus() == FeedingStatus.COMPLETED)
                .count();
    }

    private PetDashboardModel.TipOfTheDay tipFor(String petUuid, LocalDate today) {
        int index = Math.floorMod((petUuid + today).hashCode(), DAILY_TIPS.size());
        return new PetDashboardModel.TipOfTheDay(DAILY_TIPS.get(index), today);
    }

    private PetWeightLogModel toWeightModel(PetWeightLog log) {
        return new PetWeightLogModel(log.getId(), log.getWeight(), log.getRecordedAt(), log.getNote());
    }

    private PetModel toPetModel(Pet pet) {
        return new PetModel(pet.getUuid(), pet.getName(), pet.getProfilePicture(), pet.getType(), pet.getBreed(),
                pet.getDateOfBirth(), pet.getWeight(), pet.getActivityLevel(), pet.getGender(), pet.isNeutered(),
                pet.getCurrentFoodBrand(), pet.getHealthConditions(), pet.getAllergies());
    }
}
