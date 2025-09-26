package com.kittyp.article.dao;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.article.entity.ArticleComments;

public interface ArticleCommentsDao {
    
    ArticleComments saveComment(ArticleComments articleComments);

    /**
	 * 
	 * @author rrohan419@gmail.com
	 * @param pageable
	 * @return {@link Page<ArticleComments>}
	 */
	Page<ArticleComments> findAllArticlesComments(Pageable pageable, Specification<ArticleComments> specification);

	Long countCommentsByArticleId(Long articleId);

	Map<Long, Long> countCommentsByArticleIds(List<Long> articleIds);
}
