/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

/**
 * @author rrohan419@gmail.com
 */
public class AppException extends RuntimeException{

	private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
