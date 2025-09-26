package com.kittyp.article.repositry;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.article.entity.ArticlesLikes;
import com.kittyp.article.repositry.projection.ArticleIdCountProjection;

public interface ArticlesLikesRepository extends JpaRepository<ArticlesLikes, Long>, JpaSpecificationExecutor<ArticlesLikes> {
    
    long countByArticleId(Long articleId);

    @Query("select al.articleId as articleId, count(al.id) as cnt " +
           "from ArticlesLikes al " +
           "where al.articleId in :articleIds " +
           "group by al.articleId")
    List<ArticleIdCountProjection> countByArticleIds(@Param("articleIds") List<Long> articleIds);

    Optional<ArticlesLikes> findByArticleIdAndLikerUuid(Long articleId, String userUuid);

    boolean existsByArticleIdAndLikerUuid(Long articleId, String userUuid);
}
