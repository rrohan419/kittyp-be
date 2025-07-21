/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.repositry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.article.entity.Article;
import com.kittyp.article.enums.ArticleStatus;

/**
 * @author rrohan419@gmail.com 
 */
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

	Article findBySlug(String slug);
	Article findByStatus(ArticleStatus status);
	Integer countByIsActiveAndStatusIn(boolean isActive, List<ArticleStatus> status);
}
