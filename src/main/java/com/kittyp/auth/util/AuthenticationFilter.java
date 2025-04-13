package com.kittyp.auth.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
	private final AuthEntryPointJwt authEntryPointJwt;

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (shouldNotFilter(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {

			String jwt = parseJwt(request);

			if (jwtUtils.validateJwtToken(jwt)) {
				String username = jwtUtils.getUserNameFromJwtToken(jwt);

				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		} catch (AuthenticationException ex) {
			logger.error("Cannot set user authentication: {}", ex.getMessage());

			// Critical: DO NOT continue the filter chain - let Spring Security handle it
			// This ensures the exception reaches the AuthenticationEntryPoint
			SecurityContextHolder.clearContext();
			authEntryPointJwt.commence(request, response, ex);

		}
	}

	private String parseJwt(HttpServletRequest request) throws AuthenticationException {

		String headerAuth = request.getHeader("Authorization");

		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7);
		}

		throw new BadCredentialsException("Authorization token not found or invalid");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// Skip filtering for public endpoints
		String path = request.getRequestURI();
		return path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/public/") || path.startsWith("/swagger-ui/")
				|| path.startsWith("/v3/api-docs/") || path.startsWith("/actuator/");
	}

}
