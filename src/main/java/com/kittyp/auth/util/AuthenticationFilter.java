package com.kittyp.auth.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kittyp.auth.config.UserDetailServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final UserDetailServiceImpl userDetailsService;

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		try {
			String jwt = parseJwt(request);
			if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
				String username = jwtUtils.getUserNameFromJwtToken(jwt);
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception ex) {
			logger.warn("JWT authentication failed: {}", ex.getMessage());
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");
		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7).trim();
		}
		return null;
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		// Skip JWT parsing only for endpoints that never need a SecurityContext.
		// Do NOT skip /article/** or /product/** — public GETs stay permitAll in
		// SecurityConfig, but authenticated routes under those prefixes (e.g.
		// GET /article/author/me) must still receive a parsed JWT. Skipping them
		// leaves the caller anonymous, @PreAuthorize fails as 401, and the FE
		// interceptor clears a still-valid session.
		String path = request.getRequestURI();
		return path.startsWith("/api/v1/auth/")
				|| path.startsWith("/api/v1/public/")
				|| path.startsWith("/api/v1/upload/signup-documents")
				|| path.startsWith("/swagger-ui/")
				|| path.startsWith("/v3/api-docs/")
				|| path.startsWith("/actuator/")
				|| path.startsWith("/api/v1/webhook/");
	}
}
