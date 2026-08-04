package com.kittyp.nutrition.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.nutrition.dao.PetDailyPlanDao;
import com.kittyp.nutrition.dto.PetDailyPlanDto;
import com.kittyp.nutrition.entity.PetDailyPlan;
import com.kittyp.nutrition.model.PetDailyPlanModel;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PetDailyPlanServiceImpl implements PetDailyPlanService {

    private static final Logger log = LoggerFactory.getLogger(PetDailyPlanServiceImpl.class);
    private final PetDailyPlanDao petDailyPlanDao;
    private final Mapper mapper;

    @Transactional
    @Override
    public List<PetDailyPlan> createOrReplaceCurrentMonthPlan(String userUuid, String petUuid, String nutritionPlanUuid,
            List<PetDailyPlan> dailyTemplates) {

        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        int month = today.getMonthValue();
        int year = today.getYear();

        // Deactivate any old plans for this month
        deactivateExistingPlansForMonth(petUuid, month, year);

        List<PetDailyPlan> plansToSave = new ArrayList<>();

        for (LocalDate day = today; !day.isAfter(endOfMonth); day = day.plusDays(1)) {
            for (PetDailyPlan template : dailyTemplates) {
                PetDailyPlan plan = PetDailyPlan.builder()
                        .petUuid(petUuid)
                        .userUuid(userUuid)
                        .nutritionPlanUuid(nutritionPlanUuid)
                        .itemType(template.getItemType())
                        .itemName(template.getItemName())
                        .time(template.getTime())
                        .quantityInGrams(template.getQuantityInGrams())
                        .notes(template.getNotes())
                        .day(day)
                        .planMonth(month)
                        .planYear(year)
                        .active(true)
                        .build();

                plansToSave.add(plan);
            }
        }

        log.info("Generated {} daily plans for pet={} for month={} year={}",
                plansToSave.size(), petUuid, month, year);

        return petDailyPlanDao.saveAllPetDailyPlan(plansToSave);
    }

    @Override
    public void deactivateExistingPlansForMonth(String petUuid, int month, int year) {
        int updated = petDailyPlanDao.deactivatePlansForMonth(petUuid, month, year);
        if (updated > 0) {
            log.info("Deactivated {} existing plans for pet={} for {}/{}", updated, petUuid, month, year);
        }
    }

    @Override
    public List<PetDailyPlanModel> getPetsDailyPlanActivePlan(String petUuid) {
        List<PetDailyPlan> petDailyPlans = petDailyPlanDao.activePetDailyPlanByPetUuid(petUuid);
        if(petDailyPlans == null || petDailyPlans.isEmpty()){
            throw new CustomException("no active plan found for your pet", HttpStatus.NOT_FOUND);
        }

        return mapper.convertToList(petDailyPlans, PetDailyPlanModel.class);
    }

    @Override
    public List<PetDailyPlanModel> createReplaceCurrentMonthPlan(String userUuid, String petUuid,
            String nutritionPlanUuid, List<PetDailyPlanDto> dailyTemplatesDto) {
                LocalDate today = LocalDate.now();
                LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                int month = today.getMonthValue();
                int year = today.getYear();
        
                // Deactivate any old plans for this month
                deactivateExistingPlansForMonth(petUuid, month, year);
        
                List<PetDailyPlan> plansToSave = new ArrayList<>();
        
                for (LocalDate day = today; !day.isAfter(endOfMonth); day = day.plusDays(1)) {
                    for (PetDailyPlanDto template : dailyTemplatesDto) {
                        PetDailyPlan plan = PetDailyPlan.builder()
                                .petUuid(petUuid)
                                .userUuid(userUuid)
                                .nutritionPlanUuid(nutritionPlanUuid)
                                .itemType(template.getItemType())
                                .itemName(template.getItemName())
                                .time(template.getTime())
                                .quantityInGrams(template.getQuantityInGrams())
                                .notes(template.getNotes())
                                .day(day)
                                .planMonth(month)
                                .planYear(year)
                                .active(true)
                                .build();
        
                        plansToSave.add(plan);
                    }
                }
        
                log.info("Generated {} daily plans for pet={} for month={} year={}",
                        plansToSave.size(), petUuid, month, year);
        
                return mapper.convertToList(petDailyPlanDao.saveAllPetDailyPlan(plansToSave), PetDailyPlanModel.class);
    }

}
