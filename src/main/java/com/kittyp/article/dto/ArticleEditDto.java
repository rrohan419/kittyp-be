/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.article.enums.ArticleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class ArticleEditDto {

	@NotBlank
	private String title;
	
	@NotBlank
    private String excerpt;
	
	@NotBlank
    private String content;
	
    private String coverImage;
    
    @NotBlank
    private String category;
    
    
    private List<String> tags;
    
    @NotNull
    private Integer readTime;
    
    @NotNull
    private ArticleStatus status;

    /** Required when status is SCHEDULED; clear when publishing immediately. */
    private LocalDateTime scheduledPublishAt;
}
