/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.entity.Article;
import com.kittyp.article.enums.ArticleStatus;
import com.kittyp.common.constants.KeyConstant;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * @author rrohan419@gmail.com 
 */
public class ArticleSpecification {

	private ArticleSpecification() {}
	
	public static Specification<Article> articlesByFilters(ArticleFilterDto articleFilterDto) {
		return (Root<Article> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			
			predicates.add(builder.equal(root.get(KeyConstant.IS_ACTIVE), true));
			predicates.add(builder.equal(root.get(KeyConstant.ARTICLE_STATUS), ArticleStatus.PUBLISHED));
			
			if(articleFilterDto.getName() != null && !articleFilterDto.getName().isEmpty()) {
				predicates.add(builder.like(builder.lower(root.get(KeyConstant.TITLE)), "%" + articleFilterDto.getName().toLowerCase() + "%"));
			}
			
			if (articleFilterDto.getIsRandom() != null && Boolean.TRUE.equals(articleFilterDto.getIsRandom())) {
	            Expression<Double> randomFunction = builder.function("RANDOM", Double.class);
	            query.orderBy(builder.asc(randomFunction));
	        }
			
			return builder.and(predicates.toArray(new Predicate[0]));
		};
	
	}
}
