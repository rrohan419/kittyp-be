package com.kittyp.auth.util;

import com.kittyp.auth.config.UserDetailsImpl;
import com.kittyp.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUtils {

    /**
     * Get the current authenticated user's email
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Get the current authenticated user's UUID
     */
    public String getCurrentUserUuid() {
        UserDetailsImpl details = getCurrentUserDetails();
        return details != null ? details.getUuid() : null;
    }

    /**
     * Get the current authenticated user details
     */
    public UserDetailsImpl getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                return (UserDetailsImpl) principal;
            }
        }
        return null;
    }

    public boolean isAdmin() {
        UserDetailsImpl details = getCurrentUserDetails();
        if (details == null) {
            return false;
        }
        return details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_MODERATOR".equals(a));
    }

    /**
     * Ensures the caller is acting on their own resource (or is admin/moderator).
     */
    public void requireSelfOrAdmin(String userUuid) {
        String currentUuid = getCurrentUserUuid();
        if (currentUuid == null) {
            throw new CustomException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        if (!isAdmin() && (userUuid == null || !currentUuid.equals(userUuid))) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }
}
