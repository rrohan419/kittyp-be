package com.kittyp.nutrition.dto;

import java.time.LocalTime;

import com.kittyp.nutrition.enums.ItemType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetDailyPlanDto {
    
    private ItemType itemType;

    private String itemName;

    private LocalTime time;

    private Double quantityInGrams;

    private String notes;
}
