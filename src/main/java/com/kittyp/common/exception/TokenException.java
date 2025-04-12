/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

/**
 * @author rrohan419@gmail.com 
 */
public class TokenException extends AuthException {
    
    private static final long serialVersionUID = 1L;

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }

}
