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
        return materializeRange(userUuid, petUuid, nutritionPlanUuid, dailyTemplates, today, endOfMonth);
    }

    @Transactional
    @Override
    public List<PetDailyPlan> createOrReplaceNextDaysPlan(String userUuid, String petUuid, String nutritionPlanUuid,
            List<PetDailyPlan> dailyTemplates, int days) {
        int safeDays = days <= 0 ? 30 : days;
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(safeDays - 1L);
        return materializeRange(userUuid, petUuid, nutritionPlanUuid, dailyTemplates, today, end);
    }

    private List<PetDailyPlan> materializeRange(String userUuid, String petUuid, String nutritionPlanUuid,
            List<PetDailyPlan> dailyTemplates, LocalDate start, LocalDate end) {
        // Deactivate months covered by the window
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            deactivateExistingPlansForMonth(petUuid, cursor.getMonthValue(), cursor.getYear());
            cursor = cursor.plusMonths(1);
        }

        List<PetDailyPlan> plansToSave = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
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
                        .planMonth(day.getMonthValue())
                        .planYear(day.getYear())
                        .active(true)
                        .build();
                plansToSave.add(plan);
            }
        }

        log.info("Generated {} daily plans for pet={} from {} to {}",
                plansToSave.size(), petUuid, start, end);

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
            return List.of();
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
