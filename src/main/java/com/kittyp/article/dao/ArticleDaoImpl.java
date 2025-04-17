/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dao;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.article.entity.Article;
import com.kittyp.article.repositry.ArticleRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Repository
@RequiredArgsConstructor
public class ArticleDaoImpl implements ArticleDao {

	private final ArticleRepository articleRepository;
	private final Environment env;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Article saveArticle(Article article) {
		try {
			return articleRepository.save(article);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Article findArticleBySlug(String slug) {
		try {
			return articleRepository.findBySlug(slug);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR, e);

		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Page<Article> findAllArticles(Pageable pageable, Specification<Article> specification) {
		try {
			return articleRepository.findAll(specification, pageable);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
