package com.kittyp.article.repositry;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.article.entity.ArticlesLikes;

public interface ArticlesLikesRepository extends JpaRepository<ArticlesLikes, Long>, JpaSpecificationExecutor<ArticlesLikes> {
    
    long countByArticleId(Long articleId);

    Optional<ArticlesLikes> findByArticleIdAndLikerUuid(Long articleId, String userUuid);

    boolean existsByArticleIdAndLikerUuid(Long articleId, String userUuid);
}
