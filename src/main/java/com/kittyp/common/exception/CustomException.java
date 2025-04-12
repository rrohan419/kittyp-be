/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class CustomException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final int errorCode;
	private final HttpStatus httpStatus;

	public CustomException(String message, Throwable e, int errorCode, HttpStatus httpStatus) {
		super(message, e);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	public CustomException(String message, int errorCode, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}
	
	public CustomException(String message) {
		super(message);
		this.errorCode= 500;
		this.httpStatus=HttpStatus.INTERNAL_SERVER_ERROR;
	}

}
