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
 * Doctor-facing blog API aliases over the article module ({@code /api/v1/blogs}).
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class BlogController {

	private final ArticleService articleService;
	private final ApiResponse<?> responseBuilder;

	@PostMapping(ApiUrl.BLOGS_ALL)
	public ResponseEntity<SuccessResponse<PaginationModel<ArticleListModel>>> listBlogs(
			@RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) int page,
			@RequestParam(defaultValue = KeyConstant.PAGE_SIZE) int size,
			@RequestBody ArticleFilterDto filter) {
		return responseBuilder.buildSuccessResponse(
				articleService.allArticlesByFilter(filter, page, size),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.BLOGS_BY_SLUG)
	public ResponseEntity<SuccessResponse<ArticleModel>> blogBySlug(@PathVariable String slug) {
		return responseBuilder.buildSuccessResponse(
				articleService.articleBySlug(slug),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.BLOGS_BASE_URL)
	@PreAuthorize(KeyConstant.IS_ROLE_ARTICLE_PUBLISHER)
	public ResponseEntity<SuccessResponse<ArticleModel>> publishBlog(@RequestBody ArticleDto articleDto) {
		return responseBuilder.buildSuccessResponse(
				articleService.saveArticle(articleDto),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
