package com.kittyp.timezone.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.timezone.entity.TimeZoneEntry;

import jakarta.persistence.criteria.Predicate;

public final class TimeZoneSpecification {

	private TimeZoneSpecification() {
	}

	public static Specification<TimeZoneEntry> search(String search) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (search != null && !search.isBlank()) {
				String likePattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("timezoneId")), likePattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), likePattern)));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
