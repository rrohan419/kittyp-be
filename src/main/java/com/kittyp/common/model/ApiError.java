/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
	
	private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details;
    
    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    
    public ApiError(int status, String error, String message, String path, List<String> details) {
        this(status, error, message, path);
        this.details = details;
    }
}
