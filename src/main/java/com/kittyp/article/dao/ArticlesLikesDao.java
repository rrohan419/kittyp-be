package com.kittyp.article.dao;

import com.kittyp.article.entity.ArticlesLikes;

public interface ArticlesLikesDao {
    
    ArticlesLikes saveLike(ArticlesLikes articlesLikes);

    Long countArticleLikes(Long articleId);

    
}
