package com.kittyp.ai.dto;

import java.util.List;

import org.springframework.data.domain.Sort.Direction;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NutritionPlanFilter {
    
    private String petUuid;
    private String uuid;
    private String userUuid;
    private Boolean isActive;
    private String searchText;
    private List<String> tags;

    private String sortBy;
    private Direction sortDirection;

}
