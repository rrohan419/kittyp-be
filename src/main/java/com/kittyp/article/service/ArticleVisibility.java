package com.kittyp.article.service;

import com.kittyp.article.dto.ArticleFilterDto;
import com.kittyp.article.enums.ArticleStatus;

/**
 * Public article APIs must not leak drafts/scheduled/archived posts.
 * Authenticated publishers may list unpublished work, scoped to their own author
 * unless they are platform admin.
 */
final class ArticleVisibility {

	private ArticleVisibility() {
	}

	static boolean isPubliclyReadable(ArticleStatus status) {
		return status == ArticleStatus.PUBLISHED;
	}

	static void constrainListFilter(ArticleFilterDto filter, boolean publisher, boolean admin, Long ownAuthorId) {
		if (filter == null) {
			return;
		}
		if (!publisher) {
			filter.setArticleStatus(ArticleStatus.PUBLISHED);
			return;
		}
		if (admin) {
			return;
		}
		if (filter.getArticleStatus() != ArticleStatus.PUBLISHED) {
			if (ownAuthorId == null) {
				filter.setArticleStatus(ArticleStatus.PUBLISHED);
			} else {
				filter.setAuthorId(ownAuthorId);
			}
		}
	}
}
