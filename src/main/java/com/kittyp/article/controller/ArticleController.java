/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.article.dto.ArticleCommentsDto;
import com.kittyp.article.dto.ArticleDto;
import com.kittyp.article.dto.ArticleEditDto;
import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.model.ArticleCommentsModel;
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
	private final ApiResponse<?> responseBuilder;

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
	public ResponseEntity<SuccessResponse<ArticleModel>> saveArticles(
			@RequestBody ArticleDto articleDto) {

		ArticleModel response = articleService.saveArticle(articleDto);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PatchMapping(ApiUrl.ARTICLE_EDIT_BY_SLUG)
	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
	public ResponseEntity<SuccessResponse<ArticleModel>> editArticles(
			@RequestBody ArticleEditDto articleEditDto, @PathVariable String slug) {

		ArticleModel response = articleService.editArticle(slug, articleEditDto);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.ARTICLE_COMMENTS)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<PaginationModel<ArticleCommentsModel>>> articleCommentsByFilter(
			@RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) int page,
			@RequestParam(defaultValue = KeyConstant.PAGE_SIZE) int size,
			@RequestParam(required = true) Long articleId) {

		PaginationModel<ArticleCommentsModel> response = articleService.allCommentsByFilter(articleId, page, size);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.ADD_COMMENT)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<ArticleCommentsModel>> addComment(
			@RequestBody ArticleCommentsDto articleCommentsDto) {

		ArticleCommentsModel response = articleService.saveComment(articleCommentsDto);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.ADD_ARTICLE_LIKE)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<Long>> addLike(
			@PathVariable Long articleId) {

		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		Long response = articleService.addLikeToArticle(articleId, email);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PatchMapping(ApiUrl.REMOVE_ARTICLE_LIKE)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<Long>> removeLike(
			@PathVariable Long articleId) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Long response = articleService.removeLikeFromArticle(articleId, email);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.ARTICLE_LIKE_COUNT)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<Long>> articleLikeCount(
			@RequestParam(required = true) Long articleId) {

		Long response = articleService.countLikesByArticleId(articleId);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.LIKE_COMMENT)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<Long>> likeComment(
			@PathVariable Long commentId) {

		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		Long response = articleService.addLikeToComment(commentId, email);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.ARTICLE_LIKED)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<Boolean>> isArticleLikedByUser(
			@PathVariable Long articleId) {

		Boolean response = articleService.isArticleLikedByUser(articleId);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
