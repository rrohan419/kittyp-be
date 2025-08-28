package com.kittyp.article.entity;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "article_comments")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleComments extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    @Column(unique = true, nullable = false)
    private String uuid;

    @Column(columnDefinition = "TEXT")
    private String comment;
    
    private String commenterUuid;
    
    private Boolean isApproved;

    private Long articleId;
}
