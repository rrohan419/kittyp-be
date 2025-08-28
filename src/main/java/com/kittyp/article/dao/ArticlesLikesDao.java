package com.kittyp.article.dao;

import java.util.Optional;

import com.kittyp.article.entity.ArticlesLikes;

public interface ArticlesLikesDao {
    
    ArticlesLikes saveLike(ArticlesLikes articlesLikes);

    Long countArticleLikes(Long articleId);

    ArticlesLikes findByArtileLikeId(Long articleLikeId);

    Optional<ArticlesLikes> findByArtileIdAndUserUuid(Long articleId, String userUuid);

    void deleteLike(ArticlesLikes articlesLikes);

    boolean isArticleLikedByUser(Long articleId, String userUuid);
}
