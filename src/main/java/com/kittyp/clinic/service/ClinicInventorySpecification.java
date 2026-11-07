package com.kittyp.clinic.service;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.clinic.entity.ClinicInventoryItem;

public final class ClinicInventorySpecification {
    private ClinicInventorySpecification() {
    }

    public static Specification<ClinicInventoryItem> filters(Long clinicId, String category, String search) {
        return inClinic(clinicId).and(hasCategory(category)).and(matchesSearch(search));
    }

    private static Specification<ClinicInventoryItem> inClinic(Long clinicId) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("clinic").get("id"), clinicId),
                cb.isTrue(root.get("isActive")));
    }

    private static Specification<ClinicInventoryItem> hasCategory(String category) {
        if (category == null || category.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), normalized);
    }

    private static Specification<ClinicInventoryItem> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, '\\'),
                cb.like(cb.lower(root.get("category")), pattern, '\\'));
    }
}
