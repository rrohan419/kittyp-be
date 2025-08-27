package com.kittyp.article.dao;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.article.entity.ArticlesLikes;
import com.kittyp.article.repositry.ArticlesLikesRepository;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ArticlesLikesDaoImpl implements ArticlesLikesDao {

    private final ArticlesLikesRepository articlesLikesRepository;
    private final Environment env;

    @Override
    public ArticlesLikes saveLike(ArticlesLikes articlesLikes) {
       try {
			return articlesLikesRepository.save(articlesLikes);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public Long countArticleLikes(Long articleId) {
       try {
			return articlesLikesRepository.countByArticleId(articleId);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }
    
}
