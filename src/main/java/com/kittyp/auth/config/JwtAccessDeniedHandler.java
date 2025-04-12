package com.kittyp.auth.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
//        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//        response.getWriter().write("Error: Forbidden");
    	
    	 logger.warn("Access denied for request: {} {} | Reason: {}", 
                 request.getMethod(), 
                 request.getRequestURI(), 
                 accessDeniedException.getMessage());

         response.setStatus(HttpServletResponse.SC_FORBIDDEN);
         response.setContentType("application/json");

         Map<String, Object> responseBody = new HashMap<>();
         responseBody.put("timestamp", LocalDateTime.now());
         responseBody.put("status", HttpServletResponse.SC_FORBIDDEN);
         responseBody.put("error", "Forbidden");
         responseBody.put("message", accessDeniedException.getMessage());
         responseBody.put("path", request.getRequestURI());

         response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}