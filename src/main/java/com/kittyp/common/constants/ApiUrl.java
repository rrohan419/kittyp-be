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
	
	//auth controller
	public static final String AUTH_BASE_URL = "/auth";
	public static final String SIGNUP = AUTH_BASE_URL+"/signup";
	public static final String SIGNIN = AUTH_BASE_URL+"/signin";
	
	//user controller
	public static final String USER_BASE_URL = "/user";
	public static final String USER_DETAILS = USER_BASE_URL + "/me";
}
