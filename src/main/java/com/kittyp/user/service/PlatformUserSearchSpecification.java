package com.kittyp.user.service;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.common.util.SearchPredicates;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.ERole;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class PlatformUserSearchSpecification {
    private PlatformUserSearchSpecification() {
    }

    public enum SearchMode {
        GENERAL, EMAIL_OR_ID, APPOINTMENT
    }

    public static Specification<User> search(String query, SearchMode mode) {
        return (root, criteria, cb) -> {
            criteria.distinct(true);
            Predicate active = cb.and(cb.or(cb.isNull(root.get("isActive")), cb.isTrue(root.get("isActive"))),
                    cb.isTrue(root.get("enabled")),
                    cb.equal(root.join("userRoles").join("role").get("name"), ERole.ROLE_USER));
            Predicate emailOrId = cb.or(SearchPredicates.containsIgnoreCase(cb, root.get("email"), query),
                    SearchPredicates.containsIgnoreCase(cb, root.get("uuid"), query));
            Predicate match = emailOrId;
            if (mode != SearchMode.EMAIL_OR_ID) {
                match = cb.or(match, SearchPredicates.nameMatches(cb, root, query, mode == SearchMode.APPOINTMENT));
                if (mode == SearchMode.GENERAL) {
                    match = cb.or(match, cb.like(cb.coalesce(root.get("phoneNumber"), ""), "%" + query + "%"));
                } else {
                    Subquery<Long> pets = criteria.subquery(Long.class);
                    Root<Pet> pet = pets.from(Pet.class);
                    pets.select(pet.get("id")).where(
                            cb.or(cb.equal(pet.get("parentUserUuid"), root.get("uuid")),
                                    cb.<Pet, Collection<Pet>>isMember(pet, root.get("pets"))),
                            cb.isTrue(pet.get("isActive")),
                            cb.or(cb.isNull(pet.get("hiddenFromParent")), cb.isFalse(pet.get("hiddenFromParent"))),
                            cb.or(cb.isNull(pet.get("patientNumber")), cb.notLike(pet.get("patientNumber"), "doc:%")),
                            cb.or(SearchPredicates.containsIgnoreCase(cb, pet.get("name"), query),
                                    SearchPredicates.containsIgnoreCase(cb, pet.get("uuid"), query)));
                    match = cb.or(match, cb.exists(pets));
                }
            }
            return cb.and(active, match);
        };
    }
}
