package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthHtmlRedirectFilterTest {

	@Test
	void chromeNavigationWantsHtml() {
		assertTrue(HealthHtmlRedirectFilter.wantsHtml(
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
	}

	@Test
	void jsonClientsStayOnHealth() {
		assertFalse(HealthHtmlRedirectFilter.wantsHtml("application/json"));
	}

	@Test
	void curlStarStayOnHealth() {
		assertFalse(HealthHtmlRedirectFilter.wantsHtml("*/*"));
	}
}
