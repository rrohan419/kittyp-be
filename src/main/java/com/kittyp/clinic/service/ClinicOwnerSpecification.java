package com.kittyp.clinic.service;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.common.util.SearchPredicates;

import jakarta.persistence.criteria.Predicate;

public final class ClinicOwnerSpecification {
    private ClinicOwnerSpecification() {
    }

    public enum SearchMode {
        GENERAL, EMAIL_OR_ID, APPOINTMENT
    }

    public static Specification<ClinicPetOwner> search(Long clinicId, String query, SearchMode mode) {
        return (root, criteria, cb) -> {
            Predicate scope = cb.and(cb.equal(root.get("clinic").get("id"), clinicId),
                    cb.isTrue(root.get("isActive")));
            Predicate email = SearchPredicates.containsIgnoreCase(cb, root.get("email"), query);
            Predicate match;
            if (mode == SearchMode.GENERAL) {
                match = cb.or(email,
                        SearchPredicates.containsIgnoreCase(cb, root.get("firstName"), query),
                        SearchPredicates.containsIgnoreCase(cb, cb.coalesce(root.get("lastName"), ""), query),
                        cb.like(root.get("phone"), "%" + query + "%"),
                        cb.like(cb.coalesce(root.get("alternatePhone"), ""), "%" + query + "%"));
            } else {
                Predicate emailOrId = cb.or(email, SearchPredicates.containsIgnoreCase(cb, root.get("uuid"), query));
                match = mode == SearchMode.EMAIL_OR_ID ? emailOrId
                        : cb.or(emailOrId, SearchPredicates.nameMatches(cb, root, query, true));
            }
            return cb.and(scope, match);
        };
    }
}
