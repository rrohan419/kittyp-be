/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.exception;

/**
 * @author rrohan419@gmail.com 
 */
public class ResourceAlreadyExistsException extends AppException {
    
    private static final long serialVersionUID = 1L;

    /** Public signup conflict. Does not name the existing role. */
    public static final String SIGN_IN_TO_CONTINUE =
            "An account with this email already exists. Sign in to continue.";
    
    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public static ResourceAlreadyExistsException signInToContinue() {
        return new ResourceAlreadyExistsException(SIGN_IN_TO_CONTINUE);
    }

    public ResourceAlreadyExistsException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

}
