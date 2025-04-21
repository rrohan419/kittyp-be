/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.dto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@JsonInclude(Include.NON_NULL)
@Data
public class ErrorResponse<T> {

	private boolean success;
	private String message;
	private String detailedMessage;
	private LocalDateTime timestamp;
	private Integer status;
	private List<T> errors;
}
