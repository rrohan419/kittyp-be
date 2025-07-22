/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.article.entity.Article;
import com.kittyp.article.enums.ArticleStatus;

/**
 * @author rrohan419@gmail.com 
 */
public interface ArticleDao {

	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param article
	 * @return
	 */
	Article saveArticle(Article article);
	
	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param id
	 * @return
	 */
	Article findArticleBySlug(String slug);
	
	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param pageable
	 * @return
	 */
	Page<Article> findAllArticles(Pageable pageable, Specification<Article> specification);

	Integer countByIsActiveAndStatusIn(boolean isActive, List<ArticleStatus> status);
}
