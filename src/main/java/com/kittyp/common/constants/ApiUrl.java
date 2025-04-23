/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.constants;

/**
 * @author rrohan419@gmail.com 
 */
public class ApiUrl {

	private ApiUrl(){}
	
	public static final String BASE_URL="api/v1";
	
	// Auth controller
	public static final String AUTH_BASE_URL = "/auth";
	public static final String SIGNUP = AUTH_BASE_URL+"/signup";
	public static final String SIGNIN = AUTH_BASE_URL+"/signin";
	
	// User controller
	public static final String USER_BASE_URL = "/user";
	public static final String USER_DETAILS = USER_BASE_URL + "/me";
	
	// Article controller
	public static final String ARTICLE_BASE_URL = "/article";
	public static final String ALL_ARTICLES = ARTICLE_BASE_URL + "/all";
	public static final String ARTICLE_BY_SLUG = ARTICLE_BASE_URL + "/{slug}";
	
	// Product controller
	public static final String PRODUCT_BASE_URL = "/product";
	public static final String ALL_PRODUCT = PRODUCT_BASE_URL + "/all";
	public static final String PRODUCT_BY_UUID = PRODUCT_BASE_URL + "/{uuid}";
	
	// Order controller
	public static final String ORDER_BASE_URL = "/order";
	public static final String ORDER_CREATE = ORDER_BASE_URL+"/create";
	public static final String ORDER_STATUS_UPDATE = ORDER_BASE_URL+"/update/status";
	public static final String ORDERS_BY_USER = ORDER_BASE_URL+"/{userUuid}";
}
