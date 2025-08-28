package com.kittyp.article.repositry;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.article.entity.ArticleCommentLikes;

public interface ArticleCommentLikesRepository extends JpaRepository<ArticleCommentLikes, Long>{
    Long countByCommentId(Long commentId);
}
