package com.kittyp.article.dao;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.article.entity.ArticleCommentLikes;
import com.kittyp.article.repositry.ArticleCommentLikesRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ArticleCommentLikesDaoImpl implements ArticleCommentLikesDao {

    private final Environment env;
    private final ArticleCommentLikesRepository articleCommentLikesRepository;

    @Override
    public ArticleCommentLikes saveArticleCommentLike(ArticleCommentLikes articleCommentLikes) {
        try {
			return articleCommentLikesRepository.save(articleCommentLikes);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public Long countLikesByCommentId(Long commentId) {
        try {
			return articleCommentLikesRepository.countByCommentId(commentId);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }
    
}
