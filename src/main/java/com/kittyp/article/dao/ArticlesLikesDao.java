package com.kittyp.article.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kittyp.article.entity.ArticlesLikes;

public interface ArticlesLikesDao {
    
    ArticlesLikes saveLike(ArticlesLikes articlesLikes);

    Long countArticleLikes(Long articleId);

    Map<Long, Long> countArticleLikesByIds(List<Long> articleIds);

    ArticlesLikes findByArtileLikeId(Long articleLikeId);

    Optional<ArticlesLikes> findByArtileIdAndUserUuid(Long articleId, String userUuid);

    void deleteLike(ArticlesLikes articlesLikes);

    boolean isArticleLikedByUser(Long articleId, String userUuid);
}
