package com.kittyp.nutrition.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.nutrition.entity.PetDailyPlan;
import com.kittyp.nutrition.repository.PetDailyPlanRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PetDailyPlanDaoImpl implements PetDailyPlanDao{

    private final PetDailyPlanRepository petDailyPlanRepository;
    private final Environment env;

    @Override
    public int deactivatePlansForMonth(String petUuid, int month, int year) {
        try {
			return petDailyPlanRepository.deactivatePlansForMonth(petUuid, month, year);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public List<String> petUuidsWithActivePlans() {
       try {
			return petDailyPlanRepository.findDistinctPetUuidsWithActivePlans();
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public List<PetDailyPlan> saveAllPetDailyPlan(List<PetDailyPlan> petDailyPlans) {
       try {
			return petDailyPlanRepository.saveAll(petDailyPlans);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

	@Override
	public List<PetDailyPlan> activePetDailyPlanByPetUuid(String petUuid) {
		try {
			return petDailyPlanRepository.findAllByPetUuidAndIsActiveTrue(petUuid);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
}
