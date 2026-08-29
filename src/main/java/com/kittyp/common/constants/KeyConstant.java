/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.constants;

/**
 * @author rrohan419@gmail.com 
 */
public class KeyConstant {

	private KeyConstant() {}
	
	
	public static final String IS_AUTHENTICATED = "isAuthenticated()";
	public static final String SECRET_KEY = "secret.key";
	public static final String IS_ROLE_ADMIN = "hasRole('ROLE_ADMIN')";
	public static final String IS_ROLE_ADMIN_OR_MODERATOR = "hasAnyRole('ROLE_ADMIN','ROLE_MODERATOR')";
	public static final String IS_ROLE_USER = "hasRole('ROLE_USER')";
	public static final String IS_ROLE_DOCTOR = "hasRole('ROLE_DOCTOR')";
	public static final String IS_ROLE_CLINIC_ADMIN = "hasRole('ROLE_CLINIC_ADMIN')";
	public static final String IS_ROLE_CLINIC_STAFF = "hasRole('ROLE_CLINIC_STAFF')";
	public static final String IS_ROLE_CLINIC_ADMIN_OR_STAFF = "hasAnyRole('ROLE_CLINIC_ADMIN','ROLE_CLINIC_STAFF')";
	public static final String IS_ROLE_ARTICLE_PUBLISHER = "hasAnyRole('ROLE_ADMIN','ROLE_DOCTOR','ROLE_CLINIC_ADMIN')";
	public static final String PAGE_NUMBER = "1";
	public static final String PAGE_SIZE = "10";
	
	public static final String IS_ACTIVE = "isActive";
	public static final String ARTICLE_ID = "articleId";
	public static final String TITLE = "title";
	public static final String ARTICLE_STATUS = "status";
	public static final String PRODUCT_STATUS = "status";
	public static final String PRODUCT_CATEGORY = "category";
	public static final String PRODUCT_PRICE = "price";
	public static final String PRODUCT_NAME = "name";
	public static final String UPDATED_AT = "updatedAt";
	public static final String CREATED_AT = "createdAt";
	public static final String TAGS = "tags";
	
	// shipping type
	public static final String EXPRESS_SHIPPING = "EXPRESS";
	public static final String STANDRAD_SHIPPING = "STANDARD";

	
	
}
