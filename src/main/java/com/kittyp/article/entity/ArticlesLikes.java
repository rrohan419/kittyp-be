package com.kittyp.article.entity;

import com.kittyp.common.entity.BaseEntity;

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
@Table(name = "article_likes")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticlesLikes extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    private String likerUuid;
    
    private Long articleId;
    
}
