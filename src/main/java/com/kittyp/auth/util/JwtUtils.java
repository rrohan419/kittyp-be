package com.kittyp.auth.util;

import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.kittyp.auth.config.UserDetailsImpl;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.TokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;
    
    @Value("${jwt.audience}")
    private String audience;
    
    private Key key;
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void init() {
//        try {
//            // Ensure the jwtSecret is properly base64 encoded and has sufficient length for HS512
//            // The secret should be at least 64 bytes (512 bits) for HS512
//            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
//            
//            if (keyBytes.length < 64) {
//                logger.warn("JWT secret key is too short for HS512. Using a secure generated key instead.");
//                this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
//            } else {
//                this.key = Keys.hmacShaKeyFor(keyBytes);
//            }
//            
//            logger.info("JWT signing key initialized successfully");
//        } catch (Exception e) {
//            logger.error("Failed to initialize JWT key from secret. Using a secure generated key instead.", e);
//            // Fallback to a secure generated key
//            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
//        }
    	
    	try {
            // Ensure the jwtSecret is properly base64 encoded and has sufficient length for HS512
            // The secret should be at least 64 bytes (512 bits) for HS512
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            
            if (keyBytes.length < 64) {
                logger.warn("JWT secret key is too short for HS512. Using a secure generated key instead.");
                this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            } else {
                this.key = Keys.hmacShaKeyFor(keyBytes);
            }
            
            logger.info("JWT signing key initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize JWT key from secret. Using a secure generated key instead.", e);
            // Fallback to a secure generated key
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }
    
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        
        return Jwts.builder()
                .setSubject(userPrincipal.getEmail()) // Use email for consistency with UserDetailsService
                .setAudience(audience)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String getUserNameFromJwtToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            throw new TokenException(env.getProperty(ExceptionConstant.ERROR_USERNAME_EXTRACTION), e);
        }
    }
    
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new TokenException("Invalid JWT token", e);
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
            throw new TokenException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw new TokenException("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
            throw new TokenException("JWT claims string is empty", e);
        }
    }
}