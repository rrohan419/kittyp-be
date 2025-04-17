/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import com.kittyp.article.dto.ArticleDto;
import com.kittyp.article.dto.ArticleFilterDto;
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
}
