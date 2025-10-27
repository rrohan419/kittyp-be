/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dto;

import java.util.List;

import com.kittyp.article.enums.ArticleStatus;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class ArticleFilterDto {

	private String name;
	
	private Boolean isRandom;

	private ArticleStatus articleStatus;

	private List<String> tags;
}
