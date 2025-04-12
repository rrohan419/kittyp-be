package com.kittyp.common.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.Builder;

/**
 * @author rrohan419@gmail.com 
 */
@Component
@Builder
public class ApiResponse {
    
    /**
     * @author rrohan419@gmail.com 
     * 
     * Creates a success response with data and message
     */
    public <T> ResponseEntity<SuccessResponse<T>> buildSuccessResponse(T data, String message, HttpStatus httpStatus) {
        SuccessResponse<T> response = new SuccessResponse<T>(); 
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setStatus(httpStatus.value());
        response.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(response, httpStatus);
    }
    
    /**
     * @author rrohan419@gmail.com 
     * 
     * Creates an error response
     */
    public <T> ResponseEntity<ErrorResponse<T>> buildErrorResponse(String message, String detailMessage, HttpStatus httpStatus, List<T> errors) {
    	ErrorResponse<T> response = new ErrorResponse<T>(); 
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatus(httpStatus.value());
        response.setTimestamp(LocalDateTime.now());
        response.setDetailedMessage(detailMessage);
        response.setErrors(errors);
        
        return new ResponseEntity<>(response, httpStatus);
    }
    
    public <T> ResponseEntity<Object> buildValidationErrorResponse(String message, String detail,
			List<T> validationErrorModels) {

		ErrorResponse<T> errorResponse = new ErrorResponse<>();
		errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
		errorResponse.setMessage(message);
		errorResponse.setTimestamp(LocalDateTime.now());
		errorResponse.setDetailedMessage(detail);
		errorResponse.setErrors(validationErrorModels);

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}
}
