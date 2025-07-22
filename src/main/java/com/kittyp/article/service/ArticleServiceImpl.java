/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.article.dao.ArticleDao;
import com.kittyp.article.dao.AuthorDao;
import com.kittyp.article.dto.ArticleDto;
import com.kittyp.article.dto.ArticleEditDto;
import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.entity.Article;
import com.kittyp.article.entity.Author;
import com.kittyp.article.model.ArticleListModel;
import com.kittyp.article.model.ArticleModel;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;

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
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public ArticleModel articleBySlug(String slug) {
		
		Article article = articleDao.findArticleBySlug(slug);
		
		if(article == null) {
			throw new CustomException("article not found", HttpStatus.NOT_FOUND);
		}
		
		return mapper.convert(article, ArticleModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public PaginationModel<ArticleListModel> allArticlesByFilter(ArticleFilterDto articleFilterModel, Integer pageNumber,
			Integer pageSize) {
		
		Pageable pageable = PageRequest.of(pageNumber-1, pageSize);
		
		Specification<Article> articleSpecification = ArticleSpecification.articlesByFilters(articleFilterModel);
		
		Page<Article> articlePage = articleDao.findAllArticles(pageable, articleSpecification);
		
		List<ArticleListModel> articleListModels = articlePage.getContent().stream().map(article -> mapper.convert(article, ArticleListModel.class)).toList();
		
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
		
		Author author = mapper.convert(articleDto.getAuthor(), Author.class);
		
		author = authorDao.saveAuthor(author);
		
		article.setAuthor(author);
		
		Article savedArticle = articleDao.saveArticle(article);
		
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
		
		if(article == null) {
			throw new CustomException(String.format(env.getProperty(ExceptionConstant.ARTICLE_NOT_FOUND), slug), HttpStatus.NOT_FOUND);
		}
		
		if(articleEditDto.getTitle() != null && !articleEditDto.getTitle().isEmpty()){
			article.setTitle(articleEditDto.getTitle());
		}

		if(articleEditDto.getExcerpt() != null && !articleEditDto.getExcerpt().isEmpty()){
			article.setExcerpt(articleEditDto.getExcerpt());
		}

		if(articleEditDto.getContent() != null && !articleEditDto.getContent().isEmpty()){
			article.setContent(articleEditDto.getContent());
		}

		if(articleEditDto.getCoverImage() != null && !articleEditDto.getCoverImage().isEmpty()){
			article.setCoverImage(articleEditDto.getCoverImage());
		}

		if(articleEditDto.getCategory() != null && !articleEditDto.getCategory().isEmpty()){
			article.setCategory(articleEditDto.getCategory());
		}

		if(articleEditDto.getTags() != null && !articleEditDto.getTags().isEmpty()){
			article.setTags(articleEditDto.getTags());
		}

		if(articleEditDto.getReadTime() != null){
			article.setReadTime(articleEditDto.getReadTime());
		}

		if(articleEditDto.getStatus() != null){
			article.setStatus(articleEditDto.getStatus());
		}
		
		Article savedArticle = articleDao.saveArticle(article);
		
		return mapper.convert(savedArticle, ArticleModel.class);
	}

}
