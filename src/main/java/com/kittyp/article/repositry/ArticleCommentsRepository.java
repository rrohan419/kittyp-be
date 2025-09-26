package com.kittyp.article.repositry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.article.entity.ArticleComments;
import com.kittyp.article.repositry.projection.ArticleIdCountProjection;

public interface ArticleCommentsRepository
        extends JpaRepository<ArticleComments, Long>, JpaSpecificationExecutor<ArticleComments> {

    Long countByArticleId(Long articleId);

    @Query("select ac.articleId as articleId, count(ac.id) as cnt " +
            "from ArticleComments ac " +
            "where ac.articleId in :articleIds " +
            "group by ac.articleId")
    List<ArticleIdCountProjection> countByArticleIds(@Param("articleIds") List<Long> articleIds);

}
