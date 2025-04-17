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
public class ArticleListModel {

	private String title;
    private String slug;
    private String excerpt;
    private Integer readTime;
    private LocalDateTime createdAt;
    private ArticleStatus status;
    private List<String> tags;
    private AuthorModel author;
    private String category;
    private String coverImage;
}
