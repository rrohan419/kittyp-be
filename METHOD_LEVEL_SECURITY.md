# Method-Level Security Guide

This application uses Spring Security with method-level authentication using `@PreAuthorize` annotations. You don't need to update the `SecurityConfig` every time you add a new controller.

## How It Works

1. **Default Behavior**: All endpoints require authentication by default
2. **Method-Level Control**: Use `@PreAuthorize` annotations on controller methods to control access
3. **No Filter Chain Updates**: You don't need to modify `SecurityConfig` for new controllers

## Available Security Levels

### 1. Public Endpoints (No Authentication Required)
```java
@PreAuthorize(KeyConstant.PERMIT_ALL)
@GetMapping("/public-data")
public ResponseEntity<?> getPublicData() {
    // Anyone can access this endpoint
}
```

### 2. Authenticated Users Only
```java
@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
@GetMapping("/user-data")
public ResponseEntity<?> getUserData() {
    // Only authenticated users can access
}
```

### 3. Role-Based Access
```java
@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
@PostMapping("/admin-only")
public ResponseEntity<?> adminOnly() {
    // Only users with ROLE_ADMIN can access
}

@PreAuthorize(KeyConstant.IS_ROLE_USER)
@GetMapping("/user-role")
public ResponseEntity<?> userRoleOnly() {
    // Only users with ROLE_USER can access
}
```

### 4. Deny All Access
```java
@PreAuthorize(KeyConstant.DENY_ALL)
@GetMapping("/blocked")
public ResponseEntity<?> blocked() {
    // No one can access this endpoint
}
```

## Constants Available

```java
KeyConstant.PERMIT_ALL        // "permitAll()"
KeyConstant.IS_AUTHENTICATED  // "isAuthenticated()"
KeyConstant.IS_ROLE_ADMIN     // "hasRole('ROLE_ADMIN')"
KeyConstant.IS_ROLE_USER      // "hasRole('ROLE_USER')"
KeyConstant.DENY_ALL          // "denyAll()"
```

## Examples

### Article Controller
```java
@RestController
@RequestMapping("/api/v1")
public class ArticleController {
    
    // Public - anyone can view articles
    @PreAuthorize(KeyConstant.PERMIT_ALL)
    @GetMapping("/articles")
    public ResponseEntity<?> getAllArticles() { ... }
    
    // Admin only - only admins can create articles
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    @PostMapping("/articles")
    public ResponseEntity<?> createArticle() { ... }
}
```

### User Controller
```java
@RestController
@RequestMapping("/api/v1")
public class UserController {
    
    // Authenticated users only
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    @GetMapping("/user/profile")
    public ResponseEntity<?> getUserProfile() { ... }
    
    // Admin only
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers() { ... }
}
```

## Adding New Controllers

When you add a new controller, simply:

1. **Add the controller class**
2. **Add `@PreAuthorize` annotations to methods** based on your security requirements
3. **No changes needed to `SecurityConfig`**

## Benefits

- ✅ **No filter chain updates** when adding new controllers
- ✅ **Fine-grained control** at method level
- ✅ **Easy to understand** and maintain
- ✅ **Consistent security** across the application
- ✅ **Flexible** - can mix different security levels in the same controller

## Security Flow

1. Request comes in
2. JWT token is validated (if present)
3. User authentication is established
4. Method-level security (`@PreAuthorize`) is checked
5. If authorized, method executes
6. If not authorized, `AccessDeniedException` is thrown 