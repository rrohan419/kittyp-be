package com.kittyp.article.dao;

import com.kittyp.article.entity.ArticleCommentLikes;

public interface ArticleCommentLikesDao {
    
    ArticleCommentLikes saveArticleCommentLike(ArticleCommentLikes articleCommentLikes);

    Long countLikesByCommentId(Long commentId);
}
