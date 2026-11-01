package com.kittyp.auth.util;

import java.security.Key;
import java.util.Date;
import java.util.Set;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.kittyp.auth.config.UserDetailsImpl;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.TokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;
    
    @Value("${jwt.audience}")
    private String audience;
        
    @Autowired
    private Environment env;

    
    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
    
	public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenFromEmail(userPrincipal.getEmail(), jwtExpirationMs);
    }

    public String generateTokenFromEmail(String email, int expirationMs) {
        return generateToken(email, expirationMs, KeyConstant.ACCESS_TOKEN);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, refreshTokenExpirationMs, KeyConstant.REFRESH_TOKEN);
    }

    public String generateToken(String email, long expirationMs, String tokenType) {

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email must not be empty");
        }

        if (expirationMs <= 0) {
            throw new IllegalArgumentException("Token expiration must be positive");
        }

        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .setAudience(audience)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + expirationMs))
                .claim(KeyConstant.TOKEN_TYPE_CLAIM, tokenType)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaimsFromJwtToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("JWT token must not be empty");
        }

        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserNameFromJwtToken(String token) {
        try {
            return getClaimsFromJwtToken(token).getSubject();
        } catch (Exception e) {
            throw new TokenException(
                    env.getProperty(ExceptionConstant.ERROR_USERNAME_EXTRACTION), e);
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromJwtToken(token);

            return KeyConstant.REFRESH_TOKEN.equals(
                    claims.get(KeyConstant.TOKEN_TYPE_CLAIM, String.class))
                    && isAudienceValid(claims);
        } catch (Exception e) {
            logger.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaimsFromJwtToken(token);

            return KeyConstant.ACCESS_TOKEN.equals(
                    claims.get(KeyConstant.TOKEN_TYPE_CLAIM, String.class))
                    && isAudienceValid(claims);
        } catch (Exception e) {
            logger.debug("Access token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Date getTokenExpiration(String token) {
        return getClaimsFromJwtToken(token).getExpiration();
    }

    private boolean isAudienceValid(Claims claims) {
        if (!StringUtils.hasText(audience)) {
            return true;
        }

        Object tokenAudience = claims.getAudience();

        // JJWT versions may expose the audience claim as a string or set.
        if (tokenAudience instanceof String) {
            return audience.equals(tokenAudience);
        }

        if (tokenAudience instanceof Set<?>) {
            return ((Set<?>) tokenAudience).contains(audience);
        }

        return false;
    }
    
    public boolean validateJwtToken(String authToken) {
        try {

            Claims claims = getClaimsFromJwtToken(authToken);

            if (!isAudienceValid(claims)) {
                logger.warn("JWT audience mismatch");
                throw new TokenException("Invalid JWT audience");
            }

            if (!KeyConstant.ACCESS_TOKEN.equals(
                    claims.get(KeyConstant.TOKEN_TYPE_CLAIM, String.class))) {
                logger.warn("Token is not an access token");
                throw new TokenException("Invalid token type");
            }

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
        } catch (io.jsonwebtoken.security.SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            throw new TokenException("Invalid JWT signature", e);
        }
    }
}