package com.kittyp.auth.util;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author rrohan419@gmail.com
 */
@Component
@RequiredArgsConstructor
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        logger.error("Unauthorized error: {}", authException.getMessage());

        response.setHeader("Access-Control-Allow-Origin", "*"); // or specific domain
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse<Void> errorResponse = new ErrorResponse<>();
        errorResponse.setSuccess(false);
        errorResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.setMessage(authException.getMessage());
        errorResponse.setDetailedMessage(authException.getLocalizedMessage());
        errorResponse.setTimestamp(LocalDateTime.now());

        try (OutputStream out = response.getOutputStream()) {
            objectMapper.writeValue(out, errorResponse);
            out.flush();
        }
    }

}
