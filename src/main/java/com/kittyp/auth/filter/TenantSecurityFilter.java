package com.kittyp.auth.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.auth.tenant.TenantAccessService;
import com.kittyp.common.exception.ResourceNotFoundException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TenantSecurityFilter extends OncePerRequestFilter {

	private static final String CLINIC_PREFIX = "/api/v1/clinic/";
	private static final Set<String> PUBLIC_FIRST_SEGMENTS = Set.of(
			"invites", "staff-invite", "mine", "doctors", "my-doctor-invites");

	private final TenantAccessService tenantAccessService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		String clinicUuid = clinicUuidFrom(request);
		if (clinicUuid == null) {
			filterChain.doFilter(request, response);
			return;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getPrincipal() == null
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			filterChain.doFilter(request, response);
			return;
		}
		String email = authentication.getName();
		String path = request.getRequestURI();
		try {
			tenantAccessService.requireMember(clinicUuid, email, path);
		} catch (ResourceNotFoundException ex) {
			writeJson(response, HttpServletResponse.SC_NOT_FOUND, "Not Found", ex.getMessage(), path);
			return;
		} catch (AccessDeniedException ex) {
			writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", ex.getMessage(), path);
			return;
		}
		filterChain.doFilter(request, response);
	}

	static String clinicUuidFrom(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path == null || path.isBlank()) {
			path = request.getRequestURI();
		}
		if (path == null || !path.startsWith(CLINIC_PREFIX)) {
			return null;
		}
		String rest = path.substring(CLINIC_PREFIX.length());
		int slash = rest.indexOf('/');
		String first = slash < 0 ? rest : rest.substring(0, slash);
		if (first.isBlank() || PUBLIC_FIRST_SEGMENTS.contains(first) || !isUuid(first)) {
			return null;
		}
		return first;
	}

	private static boolean isUuid(String value) {
		try {
			UUID.fromString(value);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private void writeJson(HttpServletResponse response, int status, String error, String message, String path)
			throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		Map<String, Object> body = new HashMap<>();
		body.put("timestamp", LocalDateTime.now().toString());
		body.put("status", status);
		body.put("error", error);
		body.put("message", message);
		body.put("path", path);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
