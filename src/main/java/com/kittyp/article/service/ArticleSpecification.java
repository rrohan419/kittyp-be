/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.entity.Article;
import com.kittyp.article.entity.ArticleComments;
import com.kittyp.common.constants.KeyConstant;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * @author rrohan419@gmail.com
 */
public class ArticleSpecification {

	private ArticleSpecification() {
	}

	public static Specification<Article> articlesByFilters(ArticleFilterDto articleFilterDto) {
		return (Root<Article> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			List<Predicate> predicates = new ArrayList<>();

			predicates.add(builder.equal(root.get(KeyConstant.IS_ACTIVE), true));

			if (articleFilterDto.getName() != null && !articleFilterDto.getName().isEmpty()) {
				predicates.add(builder.like(builder.lower(root.get(KeyConstant.TITLE)),
						"%" + articleFilterDto.getName().toLowerCase() + "%"));
			}

			if (articleFilterDto.getIsRandom() != null && Boolean.TRUE.equals(articleFilterDto.getIsRandom())) {
				Expression<Double> randomFunction = builder.function("RANDOM", Double.class);
				query.orderBy(builder.asc(randomFunction));
			}

			if (articleFilterDto.getArticleStatus() != null) {
				predicates
						.add(builder.equal(root.get(KeyConstant.ARTICLE_STATUS), articleFilterDto.getArticleStatus()));
			}

			if (articleFilterDto.getTags() != null && !articleFilterDto.getTags().isEmpty()) {
				Join<Article, String> tagJoin = root.join(KeyConstant.TAGS);
				predicates.add(tagJoin.in(articleFilterDto.getTags()));
				query.distinct(true);
			}

			return builder.and(predicates.toArray(new Predicate[0]));
		};

	}

	public static Specification<ArticleComments> articleCommentsByFilter(Long articleId) {
		return (Root<ArticleComments> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			List<Predicate> predicates = new ArrayList<>();

			predicates.add(builder.equal(root.get(KeyConstant.IS_ACTIVE), true));

			if (articleId != null) {
				predicates.add(builder.equal(root.get(KeyConstant.ARTICLE_ID), articleId));
			}

			return builder.and(predicates.toArray(new Predicate[0]));
		};

	}
}
