package com.kittyp.article.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.article.entity.ArticleComments;

public interface ArticleCommentsRepository extends JpaRepository<ArticleComments, Long>, JpaSpecificationExecutor<ArticleComments> {
    
    Long countByArticleId(Long articleId);
}
