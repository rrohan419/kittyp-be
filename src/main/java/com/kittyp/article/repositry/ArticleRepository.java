/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.article.entity.Article;

/**
 * @author rrohan419@gmail.com 
 */
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

	Article findBySlug(String slug);
}
