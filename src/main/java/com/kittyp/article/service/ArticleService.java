/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import com.kittyp.article.dto.ArticleCommentsDto;
import com.kittyp.article.dto.ArticleDto;
import com.kittyp.article.dto.ArticleEditDto;
import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.model.ArticleCommentsModel;
import com.kittyp.article.model.ArticleListModel;
import com.kittyp.article.model.ArticleModel;
import com.kittyp.common.model.PaginationModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface ArticleService {

	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param slug
	 * @return
	 */
	ArticleModel articleBySlug(String slug);
	
	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param articleFilterModel
	 * @return
	 */
	PaginationModel<ArticleListModel> allArticlesByFilter(ArticleFilterDto articleFilterModel, Integer pageNumber,
			Integer pageSize);
	
	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param articleDto
	 * @return
	 */
	ArticleModel saveArticle(ArticleDto articleDto);
	
	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param slug
	 * @param articleEditDto
	 * @return
	 */
	ArticleModel editArticle(String slug, ArticleEditDto articleEditDto);

	ArticleCommentsModel saveComment(ArticleCommentsDto articleCommentsDto);

	PaginationModel<ArticleCommentsModel> allCommentsByFilter(Long articleId, Integer pageNumber, Integer pageSize);

	Long countCommentsByArticleId(Long articleId);

	Long countLikesByArticleId(Long articleId);

	Long addLikeToArticle(Long articleId, String email);
}
