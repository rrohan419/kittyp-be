package com.kittyp.article.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  public Map<Long, Long> countArticleLikesByIds(List<Long> articleIds) {
    try {
      var rows = articlesLikesRepository.countByArticleIds(articleIds);
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

  @Override
  public ArticlesLikes findByArtileLikeId(Long articleLikeId) {
    try {
      return articlesLikesRepository.findById(articleLikeId)
          .orElseThrow(() -> new CustomException("like not found", HttpStatus.NOT_FOUND));
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void deleteLike(ArticlesLikes articlesLikes) {
    try {
       articlesLikesRepository.delete(articlesLikes);
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public Optional<ArticlesLikes> findByArtileIdAndUserUuid(Long articleId, String userUuid) {
   try {
      return articlesLikesRepository.findByArticleIdAndLikerUuid(articleId, userUuid);
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public boolean isArticleLikedByUser(Long articleId, String userUuid) {
   try {
      return articlesLikesRepository.existsByArticleIdAndLikerUuid(articleId, userUuid);
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
