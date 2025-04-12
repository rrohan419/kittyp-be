/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

/**
 * @author rrohan419@gmail.com 
 */
public class InvalidRequestException extends AppException {
    
    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }

}
