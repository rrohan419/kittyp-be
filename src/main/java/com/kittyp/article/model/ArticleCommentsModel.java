package com.kittyp.article.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ArticleCommentsModel {

    private Long id;

    private String content;

    private LocalDateTime createdAt;

    private Long likes;

    private Boolean liked;

    private AuthorModel author;
}
