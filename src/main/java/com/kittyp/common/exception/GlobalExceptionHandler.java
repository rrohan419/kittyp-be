package com.kittyp.common.exception;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.ErrorResponse;
import com.kittyp.common.model.ApiError;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ApiResponse responseBuilder;

	/**
	 * Extract the path from the request
	 */
	private String extractPath(WebRequest request) {
		if (request instanceof ServletWebRequest) {
			return ((ServletWebRequest) request).getRequest().getRequestURI();
		}
		return request.getDescription(false);
	}

	/**
	 * Handle custom AppException hierarchy
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND.value(), "Resource Not Found", ex.getMessage(), path);

		log.error("Resource not found exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<ApiError> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex,
			WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.CONFLICT.value(), "Resource Already Exists", ex.getMessage(), path);

		log.error("Resource already exists exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiError> handleAuthException(AuthException ex, WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Authentication Error", ex.getMessage(),
				path);

		log.error("Authentication exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(TokenException.class)
	public ResponseEntity<ApiError> handleTokenException(TokenException ex, WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Token Error", ex.getMessage(), path);

		log.error("Token exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ApiError> handleInvalidRequestException(InvalidRequestException ex, WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Invalid Request", ex.getMessage(), path);

		log.error("Invalid request exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(BusinessLogicException.class)
	public ResponseEntity<ApiError> handleBusinessLogicException(BusinessLogicException ex, WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Business Logic Error",
				ex.getMessage(), path);

		log.error("Business logic exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
	public ResponseEntity<ErrorResponse<Void>> handleAccessDeniedException(AccessDeniedException ex,
			WebRequest request) {

		return responseBuilder.buildErrorResponse("Access Denied", "You don't have permission to access this resource",
				HttpStatus.FORBIDDEN, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Authentication Failed", ex.getMessage(),
				path);

		log.error("Authentication exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Handle validation exceptions
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			WebRequest request) {

		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.toList());

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation Error", "Input validation failed",
				path, details);

		log.error("Validation exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {

		List<String> details = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.collect(Collectors.toList());

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation Error",
				"Constraint validation failed", path, details);

		log.error("Constraint violation exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle HTTP message conversion exceptions
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Malformed JSON Request",
				"JSON parse error: " + ex.getMessage(), path);

		log.error("JSON parse exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle method not allowed exceptions
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
			WebRequest request) {

		StringBuilder builder = new StringBuilder();
		builder.append(ex.getMethod());
		builder.append(" method is not supported for this request. Supported methods are ");
		ex.getSupportedHttpMethods().forEach(t -> builder.append(t).append(" "));

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.METHOD_NOT_ALLOWED.value(), "Method Not Allowed",
				builder.toString(), path);

		log.error("Method not allowed exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.METHOD_NOT_ALLOWED);
	}

	/**
	 * Handle missing servlet request parameter exception
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiError> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			WebRequest request) {

		String path = extractPath(request);
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "Missing Parameter",
				ex.getParameterName() + " parameter is missing", path);

		log.error("Missing parameter exception: {}", ex.getMessage());
		return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse<Void>> handleCustomException(CustomException ex, WebRequest request) {
//        String path = extractPath(request);
		
		return responseBuilder.buildErrorResponse(ex.getMessage(), ex.getLocalizedMessage(),
				ex.getHttpStatus(), null);
	}

	/**
	 * Handle all other exceptions
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleAll(Exception ex, WebRequest request) {
		String path = extractPath(request);

		ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error",
				"An unexpected error occurred", path);

		log.error("Unhandled exception", ex);
		return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
		
		return responseBuilder.buildErrorResponse("Invalid username or password", ex.getLocalizedMessage(),
				HttpStatus.UNAUTHORIZED, null);
	}
}