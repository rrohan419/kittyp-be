package com.kittyp.ai.service;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.ai.dto.NutritionPlanFilter;
import com.kittyp.ai.entity.NutritionPlan;

public class NutritionPlanSpecification {
    
    private NutritionPlanSpecification(){}

    public static Specification<NutritionPlan> filters(NutritionPlanFilter filter) {
        return Specification.where(
                filter.getUuid() != null ? hasUuid(filter.getUuid()) : null
        ).and(
                filter.getUserUuid() != null ? hasUserUuid(filter.getUserUuid()) : null
        ).and(
                filter.getIsActive() != null ? isActive(filter.getIsActive()) : null
        ).and(
                filter.getPetUuid() != null ? hasPetUuid(filter.getPetUuid()) : null
        ).and(
                filter.getTags() != null && !filter.getTags().isEmpty() ? hasTags(filter.getTags()) : null
        ).and(
                filter.getSearchText() != null ? searchText(filter.getSearchText()) : null
        );
    }

    public static Specification<NutritionPlan> searchText(String searchText) {
        return (root, query, criteriaBuilder) -> {
            String likePattern = "%" + searchText.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("planName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("notes")), likePattern)
            );
        };
    }


    public static Specification<NutritionPlan> hasTags(List<String> tags) {
        return (root, query, criteriaBuilder) -> {
            if (tags == null || tags.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("tags").in(tags);
        };
    }

    public static Specification<NutritionPlan> hasUuid(String uuid) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("uuid"), uuid);
    }

    public static Specification<NutritionPlan> hasUserUuid(String userUuid) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userUuid"), userUuid);
    }

    public static Specification<NutritionPlan> isActive(boolean isActive) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    public static Specification<NutritionPlan> hasPetUuid(String petUuid) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("petUuid"), petUuid);
    }
}
