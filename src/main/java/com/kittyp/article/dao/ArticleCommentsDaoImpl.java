package com.kittyp.article.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.article.entity.ArticleComments;
import com.kittyp.article.repositry.ArticleCommentsRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class ArticleCommentsDaoImpl implements ArticleCommentsDao {

    private final ArticleCommentsRepository articleCommentsRepository;
    private final Environment env;

    @Override
    public ArticleComments saveComment(ArticleComments articleComments) {
       try {
			return articleCommentsRepository.save(articleComments);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    /**
	 * 
	 * @author rrohan419@gmail.com
	 * @param pageable
	 * @return {@link Page<ArticleComments>}
	 */
    @Override
    public Page<ArticleComments> findAllArticlesComments(Pageable pageable,
            Specification<ArticleComments> specification) {
       try {
			return articleCommentsRepository.findAll(specification, pageable);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

	@Override
	public Long countCommentsByArticleId(Long articleId) {
		try {
			return articleCommentsRepository.countByArticleId(articleId);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Map<Long, Long> countCommentsByArticleIds(List<Long> articleIds) {
		try {
			var rows = articleCommentsRepository.countByArticleIds(articleIds);
			Map<Long, Long> result = new HashMap<>();
			for (var row : rows) {
				result.put(row.getArticleId(), row.getCnt());
			}
			return result;
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
