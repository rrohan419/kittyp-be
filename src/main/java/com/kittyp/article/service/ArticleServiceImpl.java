/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.service;

import java.util.List;

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
import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.entity.Article;
import com.kittyp.article.entity.Author;
import com.kittyp.article.model.ArticleListModel;
import com.kittyp.article.model.ArticleModel;
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
		
		article = articleDao.saveArticle(article);
		
		return mapper.convert(article, ArticleModel.class);
	}

}
