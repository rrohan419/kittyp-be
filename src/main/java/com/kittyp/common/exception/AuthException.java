/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

/**
 * @author rrohan419@gmail.com 
 */
public class AuthException extends AppException {
    
    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
