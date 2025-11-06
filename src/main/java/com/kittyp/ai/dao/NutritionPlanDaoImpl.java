package com.kittyp.ai.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.repository.NutritionPlanRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class NutritionPlanDaoImpl implements NutritionPlanDao {

    private final NutritionPlanRepository nutritionPlanRepository;
    private final Environment env;

    @Override
    public NutritionPlan saveNutritionPlan(NutritionPlan nutritionPlan) {
        try {
            return nutritionPlanRepository.save(nutritionPlan);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.name(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public NutritionPlan getNutritionPlanByUuid(String uuid) {
        return nutritionPlanRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException(
                        String.format(env.getProperty(ExceptionConstant.NUTRITION_PLAN_NOT_FOUND), uuid),
                        HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<NutritionPlan> getNutritionPlansByPetUuid(String petUuid, boolean isActive, Pageable pageable) {
        try {
            return nutritionPlanRepository.findByPetUuidAndIsActive(petUuid, isActive, pageable);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.name(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<NutritionPlan> getNutritionPlansByUserUuid(String userUuid, boolean isActive) {
        try {
            return nutritionPlanRepository.findByUserUuidAndIsActive(userUuid, isActive);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.name(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Optional<NutritionPlan> getNutritionPlansByPetUuidAndIsActiveTrue(String petUuid) {
        throw new UnsupportedOperationException("Unimplemented method 'getNutritionPlansByPetUuidAndIsActiveTrue'");
    }

    @Override
    public Page<NutritionPlan> findAllByFilter(Specification<NutritionPlan> specification, Pageable pageable) {
        try {
            return nutritionPlanRepository.findAll(specification, pageable);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.name(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
