package com.kittyp.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.enums.ArticleStatus;

class ArticleVisibilityTest {

	@Test
	void onlyPublishedIsPubliclyReadable() {
		assertTrue(ArticleVisibility.isPubliclyReadable(ArticleStatus.PUBLISHED));
		assertFalse(ArticleVisibility.isPubliclyReadable(ArticleStatus.DRAFT));
		assertFalse(ArticleVisibility.isPubliclyReadable(ArticleStatus.SCHEDULED));
		assertFalse(ArticleVisibility.isPubliclyReadable(ArticleStatus.ARCHIVED));
		assertFalse(ArticleVisibility.isPubliclyReadable(null));
	}

	@Test
	void anonymousFilterForcedToPublished() {
		ArticleFilterDto filter = new ArticleFilterDto();
		filter.setArticleStatus(ArticleStatus.DRAFT);
		filter.setAuthorId(9L);
		ArticleVisibility.constrainListFilter(filter, false, false, 3L);
		assertEquals(ArticleStatus.PUBLISHED, filter.getArticleStatus());
		assertEquals(9L, filter.getAuthorId());
	}

	@Test
	void anonymousNullStatusForcedToPublished() {
		ArticleFilterDto filter = new ArticleFilterDto();
		ArticleVisibility.constrainListFilter(filter, false, false, null);
		assertEquals(ArticleStatus.PUBLISHED, filter.getArticleStatus());
	}

	@Test
	void nonAdminPublisherDraftsScopedToOwnAuthor() {
		ArticleFilterDto filter = new ArticleFilterDto();
		filter.setArticleStatus(ArticleStatus.DRAFT);
		filter.setAuthorId(99L);
		ArticleVisibility.constrainListFilter(filter, true, false, 7L);
		assertEquals(ArticleStatus.DRAFT, filter.getArticleStatus());
		assertEquals(7L, filter.getAuthorId());
	}

	@Test
	void nonAdminPublisherNullStatusScopedToOwnAuthor() {
		ArticleFilterDto filter = new ArticleFilterDto();
		ArticleVisibility.constrainListFilter(filter, true, false, 7L);
		assertNull(filter.getArticleStatus());
		assertEquals(7L, filter.getAuthorId());
	}

	@Test
	void nonAdminPublisherCanBrowseAllPublished() {
		ArticleFilterDto filter = new ArticleFilterDto();
		filter.setArticleStatus(ArticleStatus.PUBLISHED);
		filter.setAuthorId(null);
		ArticleVisibility.constrainListFilter(filter, true, false, 7L);
		assertEquals(ArticleStatus.PUBLISHED, filter.getArticleStatus());
		assertNull(filter.getAuthorId());
	}

	@Test
	void publisherWithoutAuthorProfileCannotListDrafts() {
		ArticleFilterDto filter = new ArticleFilterDto();
		filter.setArticleStatus(ArticleStatus.DRAFT);
		ArticleVisibility.constrainListFilter(filter, true, false, null);
		assertEquals(ArticleStatus.PUBLISHED, filter.getArticleStatus());
	}

	@Test
	void adminKeepsRequestedFilter() {
		ArticleFilterDto filter = new ArticleFilterDto();
		filter.setArticleStatus(ArticleStatus.DRAFT);
		filter.setAuthorId(null);
		ArticleVisibility.constrainListFilter(filter, true, true, 7L);
		assertEquals(ArticleStatus.DRAFT, filter.getArticleStatus());
		assertNull(filter.getAuthorId());
	}
}
