/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.article.dto.ArticleDto;
import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.model.ArticleListModel;
import com.kittyp.article.model.ArticleModel;
import com.kittyp.article.service.ArticleService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ArticleController {

	private final ArticleService articleService;
	private final ApiResponse responseBuilder;
	
	@PostMapping(ApiUrl.ALL_ARTICLES)
    public ResponseEntity<SuccessResponse<PaginationModel<ArticleListModel>>> allArticlesByFilter(
    		@RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) int page,
			@RequestParam(defaultValue = KeyConstant.PAGE_SIZE) int size,
			@RequestBody ArticleFilterDto articleFilterDto) {
        		
        PaginationModel<ArticleListModel> response = articleService.allArticlesByFilter(articleFilterDto, page, size);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@GetMapping(ApiUrl.ARTICLE_BY_SLUG)
	public ResponseEntity<SuccessResponse<ArticleModel>> articlesBySlug(
    		@PathVariable String slug) {
        		
		ArticleModel response = articleService.articleBySlug(slug);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@PostMapping(ApiUrl.ARTICLE_BASE_URL)
	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    public ResponseEntity<SuccessResponse<ArticleModel>> savearticles(
			@RequestBody ArticleDto articleDto) {
        		
        ArticleModel response = articleService.saveArticle(articleDto);
        
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
