/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.kittyp.article.dao.ArticleCommentsDao;
import com.kittyp.article.dao.ArticleDao;
import com.kittyp.article.dao.ArticlesLikesDao;
import com.kittyp.article.dao.AuthorDao;
import com.kittyp.article.dto.ArticleCommentsDto;
import com.kittyp.article.dto.ArticleDto;
import com.kittyp.article.dto.ArticleEditDto;
import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.entity.Article;
import com.kittyp.article.entity.ArticleComments;
import com.kittyp.article.entity.ArticlesLikes;
import com.kittyp.article.entity.Author;
import com.kittyp.article.model.ArticleCommentsModel;
import com.kittyp.article.model.ArticleListModel;
import com.kittyp.article.model.ArticleModel;
import com.kittyp.article.model.AuthorModel;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

	private final ArticleDao articleDao;
	private final Mapper mapper;
	private final AuthorDao authorDao;
	private final Environment env;
	private final UserDao userDao;
	private final ArticleCommentsDao articleCommentsDao;
	private final ArticlesLikesDao articlesLikesDao;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public ArticleModel articleBySlug(String slug) {

		Article article = articleDao.findArticleBySlug(slug);

		if (article == null) {
			throw new CustomException("article not found", HttpStatus.NOT_FOUND);
		}

		ArticleModel articleModel = mapper.convert(article, ArticleModel.class);
		articleModel.setLikeCount(articlesLikesDao.countArticleLikes(articleModel.getId()));
		articleModel.setCommentCount(articleCommentsDao.countCommentsByArticleId(articleModel.getId()));
		return articleModel;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public PaginationModel<ArticleListModel> allArticlesByFilter(ArticleFilterDto articleFilterModel,
			Integer pageNumber,
			Integer pageSize) {

		Sort sort = Sort.by(Direction.DESC, KeyConstant.CREATED_AT);
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

		Specification<Article> articleSpecification = ArticleSpecification.articlesByFilters(articleFilterModel);

		Page<Article> articlePage = articleDao.findAllArticles(pageable, articleSpecification);

		List<ArticleListModel> articleListModels = articlePage.getContent().stream().map(article -> {
			ArticleListModel model = mapper.convert(article, ArticleListModel.class);
			Long articleId = article.getId();
			model.setLikeCount(articlesLikesDao.countArticleLikes(articleId));
			model.setCommentCount(articleCommentsDao.countCommentsByArticleId(articleId));
			return model;
		}).toList();

		return articlePageToModel(new PageImpl<>(articleListModels, pageable, articlePage.getTotalElements()));
	}

	private PaginationModel<ArticleListModel> articlePageToModel(Page<ArticleListModel> articlePage) {
		PaginationModel<ArticleListModel> projectModel = new PaginationModel<>();
		projectModel.setModels(articlePage.getContent());
		projectModel.setIsFirst(articlePage.isFirst());
		projectModel.setIsLast(articlePage.isLast());
		projectModel.setTotalElements(articlePage.getTotalElements());
		projectModel.setTotalPages(articlePage.getTotalPages());
		return projectModel;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public ArticleModel saveArticle(ArticleDto articleDto) {
		Article article = mapper.convert(articleDto, Article.class);

		Author author = authorDao.authorById(articleDto.getAuthorId());

		article.setAuthor(author);

		Article savedArticle = articleDao.saveArticle(article);

		author.getArticles().add(savedArticle);
		authorDao.saveAuthor(author);

		return mapper.convert(savedArticle, ArticleModel.class);
	}

	/**
	 * 
	 * @author rrohan419@gmail.com
	 * @param slug
	 * @param articleEditDto
	 * @return
	 */
	@Override
	public ArticleModel editArticle(String slug, ArticleEditDto articleEditDto) {
		Article article = articleDao.findArticleBySlug(slug);

		if (article == null) {
			throw new CustomException(String.format(env.getProperty(ExceptionConstant.ARTICLE_NOT_FOUND), slug),
					HttpStatus.NOT_FOUND);
		}

		if (articleEditDto.getTitle() != null && !articleEditDto.getTitle().isEmpty()) {
			article.setTitle(articleEditDto.getTitle());
		}

		if (articleEditDto.getExcerpt() != null && !articleEditDto.getExcerpt().isEmpty()) {
			article.setExcerpt(articleEditDto.getExcerpt());
		}

		if (articleEditDto.getContent() != null && !articleEditDto.getContent().isEmpty()) {
			article.setContent(articleEditDto.getContent());
		}

		if (articleEditDto.getCoverImage() != null && !articleEditDto.getCoverImage().isEmpty()) {
			article.setCoverImage(articleEditDto.getCoverImage());
		}

		if (articleEditDto.getCategory() != null && !articleEditDto.getCategory().isEmpty()) {
			article.setCategory(articleEditDto.getCategory());
		}

		if (articleEditDto.getTags() != null && !articleEditDto.getTags().isEmpty()) {
			article.setTags(articleEditDto.getTags());
		}

		if (articleEditDto.getReadTime() != null) {
			article.setReadTime(articleEditDto.getReadTime());
		}

		if (articleEditDto.getStatus() != null) {
			article.setStatus(articleEditDto.getStatus());
		}

		Article savedArticle = articleDao.saveArticle(article);

		return mapper.convert(savedArticle, ArticleModel.class);
	}

	@Override
	public ArticleCommentsModel saveComment(ArticleCommentsDto articleCommentsDto) {

		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userDao.userByEmail(email);
		ArticleComments articleComments = mapper.convert(articleCommentsDto, ArticleComments.class);
		articleComments.setUuid(UUID.randomUUID().toString());
		articleComments.setIsApproved(Boolean.FALSE);
		articleComments.setCommenterUuid(user.getUuid());

		ArticleComments savedComment = articleCommentsDao.saveComment(articleComments);

		AuthorModel author = new AuthorModel();
		author.setName(user.getFirstName() + " " + user.getLastName());
		author.setAvatar(user.getProfilePictureUrl());
		author.setRole("User");

		ArticleCommentsModel model = new ArticleCommentsModel();
		model.setAuthor(author);
		model.setContent(savedComment.getComment());
		model.setCreatedAt(savedComment.getCreatedAt());
		model.setLiked(Boolean.TRUE);
		model.setLikes(18L);
		model.setId(savedComment.getId());
		return model;

	}

	@Override
	public PaginationModel<ArticleCommentsModel> allCommentsByFilter(Long articleId, Integer pageNumber,
			Integer pageSize) {

		Sort sort = Sort.by(Direction.DESC, KeyConstant.CREATED_AT);
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

		Specification<ArticleComments> articleCommentsSpecification = ArticleSpecification
				.articleCommentsByFilter(articleId);

		Page<ArticleComments> articleCommentsPage = articleCommentsDao.findAllArticlesComments(pageable,
				articleCommentsSpecification);

		List<ArticleCommentsModel> articleListModels = articleCommentsPage.getContent().stream()
				.map(articleComment -> {
					User user = userDao.userByUuid(articleComment.getCommenterUuid());
					
					AuthorModel author = new AuthorModel();
					author.setName(user.getFirstName() + " " + user.getLastName());
					author.setAvatar(user.getProfilePictureUrl());
					author.setRole("User");

					ArticleCommentsModel articleCommentsModel = mapper.convert(articleComment, ArticleCommentsModel.class);
					articleCommentsModel.setAuthor(author);
					articleCommentsModel.setContent(articleComment.getComment());
					return articleCommentsModel;
				}).toList();

		return articleCommentsPageToModel(
				new PageImpl<>(articleListModels, pageable, articleCommentsPage.getTotalElements()));
	}

	/**
	 * @author rrohan419@gmail.com
	 * @param articleCommentsPage
	 * @return {@link PaginationModel<ArticleCommentsDto>}
	 */
	private PaginationModel<ArticleCommentsModel> articleCommentsPageToModel(
			Page<ArticleCommentsModel> articleCommentsPage) {
		PaginationModel<ArticleCommentsModel> projectModel = new PaginationModel<>();
		projectModel.setModels(mapper.convertToList(articleCommentsPage.getContent(), ArticleCommentsModel.class));
		projectModel.setIsFirst(articleCommentsPage.isFirst());
		projectModel.setIsLast(articleCommentsPage.isLast());
		projectModel.setTotalElements(articleCommentsPage.getTotalElements());
		projectModel.setTotalPages(articleCommentsPage.getTotalPages());
		return projectModel;
	}

	@Override
	public Long countCommentsByArticleId(Long articleId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'countCommentsByArticleId'");
	}

	@Override
	public Long countLikesByArticleId(Long articleId) {
		return articlesLikesDao.countArticleLikes(articleId);
	}

	@Override
	public Long addLikeToArticle(Long articleId, String email) {
		User user = userDao.userByEmail(email);
		ArticlesLikes articlesLikes = new ArticlesLikes();
		articlesLikes.setArticleId(articleId);
		articlesLikes.setLikerUuid(user.getUuid());
		articlesLikesDao.saveLike(articlesLikes);

		return articlesLikesDao.countArticleLikes(articleId);

	}

}
