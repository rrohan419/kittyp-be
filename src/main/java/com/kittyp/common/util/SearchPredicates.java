package com.kittyp.common.util;

import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class SearchPredicates {
    private SearchPredicates() {
    }

    public static Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> field, String query) {
        return cb.like(cb.lower(field), "%" + query.toLowerCase(Locale.ROOT) + "%");
    }

    public static Predicate nameMatches(CriteriaBuilder cb, Root<?> root, String query, boolean ignoreSpaces) {
        Expression<String> first = cb.coalesce(root.get("firstName"), "");
        Expression<String> last = cb.coalesce(root.get("lastName"), "");
        Expression<String> fullName = cb.concat(cb.concat(first, " "), last);
        Predicate names = cb.or(containsIgnoreCase(cb, first, query), containsIgnoreCase(cb, last, query),
                containsIgnoreCase(cb, fullName, query));
        if (!ignoreSpaces) {
            return names;
        }
        Expression<String> compactName = cb.function("replace", String.class,
                cb.lower(cb.concat(first, last)), cb.literal(" "), cb.literal(""));
        return cb.or(names, cb.like(compactName, "%" + query.toLowerCase(Locale.ROOT).replace(" ", "") + "%"));
    }
}
