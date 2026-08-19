package com.kittyp.common.health;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class HealthHtmlRedirectFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (wantsHtml(request.getHeader("Accept")) && isHealthPath(request.getRequestURI())) {
			response.sendRedirect(request.getContextPath() + "/actuator/dashboard");
			return;
		}
		filterChain.doFilter(request, response);
	}

	static boolean wantsHtml(String accept) {
		if (accept == null) {
			return false;
		}
		return accept.contains("text/html") && !accept.contains("application/json")
				&& !accept.contains("application/vnd.spring-boot");
	}

	private static boolean isHealthPath(String uri) {
		return "/actuator/health".equals(uri) || uri.endsWith("/actuator/health");
	}
}
