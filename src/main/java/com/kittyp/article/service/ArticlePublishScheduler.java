package com.kittyp.article.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Publishes articles that were scheduled for a specific date/time.
 */
@Component
@RequiredArgsConstructor
public class ArticlePublishScheduler {

	private static final Logger log = LoggerFactory.getLogger(ArticlePublishScheduler.class);

	private final ArticleService articleService;

	@Scheduled(fixedDelayString = "${kittyp.article.publish-check-ms:60000}")
	public void publishDueArticles() {
		int published = articleService.publishDueScheduledArticles();
		if (published > 0) {
			log.info("Published {} scheduled article(s)", published);
		}
	}
}
