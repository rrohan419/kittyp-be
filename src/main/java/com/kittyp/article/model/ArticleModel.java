/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.model;

import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.article.enums.ArticleStatus;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class ArticleModel {

	private Long id;
    private String title;
    private String slug;
    private LocalDateTime createdAt;
    private String excerpt;
    private String content;
    private String coverImage;
    private String category;
    private ArticleStatus status;
    private List<String> tags;
    private Integer readTime;
    private AuthorModel author;
     private Long commentCount;
    private Long likeCount;
}
