/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.constants;

/**
 * @author rrohan419@gmail.com
 */
public class ApiUrl {

	private ApiUrl() {
	}

	public static final String BASE_URL = "api/v1";
	public static final String ADMIN = "/admin";
	public static final String PATH_VARIABLE_UUID = "/{uuid}";

	// Auth controller
	public static final String AUTH_BASE_URL = "/auth";
	public static final String SIGNUP = AUTH_BASE_URL + "/signup";
	public static final String SIGNIN = AUTH_BASE_URL + "/signin";
	public static final String SOCIAL_SSO = AUTH_BASE_URL + "/social-sso";
	public static final String SEND_CODE = AUTH_BASE_URL + "/send-code";
	public static final String VERIFY_CODE = AUTH_BASE_URL + "/verify-code";
	public static final String USER_PASSWORD_RESET = AUTH_BASE_URL + "/password-reset";

	// User controller
	public static final String USER_BASE_URL = "/user";
	public static final String USER_DETAILS = USER_BASE_URL + "/me";
	public static final String USER_ADDRESS = USER_BASE_URL + "/address";
	public static final String USER_ADDRESS_DETAIL = USER_BASE_URL + "/address/detail";

	// Article controller
	public static final String ARTICLE_BASE_URL = "/article";
	public static final String ALL_ARTICLES = ARTICLE_BASE_URL + "/all";
	public static final String ARTICLE_BY_SLUG = ARTICLE_BASE_URL + "/{slug}";
	public static final String ARTICLE_EDIT_BY_SLUG = ADMIN + ARTICLE_BASE_URL + "/edit/{slug}";
	public static final String ARTICLE_COMMENTS = "/comments";
	public static final String ADD_COMMENT = "/comment/add";
	public static final String ADD_ARTICLE_LIKE = "/like/add/{articleId}";
	public static final String REMOVE_ARTICLE_LIKE = "/like/remove/{articleId}";
	public static final String ARTICLE_LIKE_COUNT = "/like/count";
	public static final String LIKE_COMMENT = "/like/comment/{commentId}";
	public static final String ARTICLE_LIKED = "/like/user/{articleId}";

	// Author controller
	public static final String AUTHOR_BASE_URL = "/author";
	public static final String ALL_AUTHORS = ARTICLE_BASE_URL + AUTHOR_BASE_URL + "/all";
	public static final String AUTHOR_BY_ID = AUTHOR_BASE_URL + "/{id}";
	public static final String CREATE_AUTHOR = ADMIN + AUTHOR_BASE_URL + "/create";

	// Product controller
	public static final String PRODUCT_BASE_URL = "/product";
	public static final String ALL_PRODUCT = PRODUCT_BASE_URL + "/all";
	public static final String PRODUCT_BY_UUID = PRODUCT_BASE_URL + PATH_VARIABLE_UUID;
	public static final String PRODUCT_COUNT = ADMIN + PRODUCT_BASE_URL + "/count";
	public static final String DELETE_PRODUCT = ADMIN + PRODUCT_BASE_URL + "/{productUuid}";
	public static final String UPDATE_PRODUCT = ADMIN + PRODUCT_BASE_URL + "/update";
	// Order controller
	public static final String ORDER_BASE_URL = "/order";
	public static final String ORDER_CREATE = ORDER_BASE_URL + "/create";
	public static final String ORDER_STATUS_UPDATE = ORDER_BASE_URL + "/update/status";
	public static final String ORDERS_BY_FILTER = ORDER_BASE_URL + "/filter";
	public static final String CREATED_ORDER_BY_USER = ORDER_BASE_URL + "/created/{userUuid}";
	public static final String ORDER_INVOICE_BY_USER = ORDER_BASE_URL + "/invoice/{orderNumber}";
	public static final String ORDER_CHECKOUT = ORDER_BASE_URL + "/checkout/{userUuid}";
	public static final String SUCCESSFULL_ORDERS_BY_USER = ORDER_BASE_URL + "/count";

	// cart controller
	public static final String CART_BASE_URL = "/cart";
	public static final String GET_CART_BY_USER = CART_BASE_URL + "/get/{userUuid}";
	public static final String ADD_TO_CART = CART_BASE_URL + "/add/{userUuid}";
	public static final String REMOVE_FROM_CART = CART_BASE_URL + "/remove/{userUuid}/{productUuid}";
	public static final String CLEAR_CART = CART_BASE_URL + "/clear/{userUuid}";

	public static final String UPDATE_CART_ITEM = CART_BASE_URL + "/update/{userUuid}";

	// public controller
	public static final String PUBLIC_BASE_URL = "/public";
	public static final String SITEMAP = PUBLIC_BASE_URL + "/sitemap.xml";

	// pet controller
	public static final String PET_BASE_URL = "/pet";
	public static final String PET_BY_UUID = PET_BASE_URL + PATH_VARIABLE_UUID;
}
