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
	public static final String SIGNUP_DOCTOR = SIGNUP + "/doctor";
	public static final String SIGNUP_CLINIC = SIGNUP + "/clinic";
	public static final String SIGNUP_OTP_SEND = SIGNUP + "/otp/send";
	public static final String SIGNUP_OTP_VERIFY = SIGNUP + "/otp/verify";
	public static final String SIGNIN = AUTH_BASE_URL + "/signin";
	public static final String SOCIAL_SSO = AUTH_BASE_URL + "/social-sso";
	public static final String SEND_CODE = AUTH_BASE_URL + "/send-code";
	public static final String VERIFY_CODE = AUTH_BASE_URL + "/verify-code";
	public static final String USER_PASSWORD_RESET = AUTH_BASE_URL + "/password-reset";

	public static final String ADMIN_DOCTORS = ADMIN + "/doctors";
	public static final String ADMIN_DOCTOR_BY_UUID = ADMIN_DOCTORS + PATH_VARIABLE_UUID;
	public static final String ADMIN_DOCTOR_STATUS = ADMIN_DOCTOR_BY_UUID + "/status";
	public static final String ADMIN_DOCTOR_CHECKLIST = ADMIN_DOCTOR_BY_UUID + "/checklist";

	public static final String DOCTOR_BASE_URL = "/doctor";
	public static final String DOCTOR_ME = DOCTOR_BASE_URL + "/me";
	public static final String DOCTOR_ME_AVAILABILITY = DOCTOR_ME + "/availability";

	public static final String UPLOAD_SIGNUP_DOCUMENTS = "/upload/signup-documents";

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
	public static final String ARTICLE_COMMENTS = ARTICLE_BASE_URL + "/comments";
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
	public static final String PET_WEIGHT = PET_BY_UUID + "/weight";
	public static final String PET_WEIGHT_HISTORY = PET_BY_UUID + "/weight-history";
	public static final String PET_DASHBOARD = PET_BY_UUID + "/dashboard";

	// nutrition controller
	public static final String NUTRITION_PET_BASE_URL = "/nutrition/pets";
	public static final String PET_FEEDING_LOGS = NUTRITION_PET_BASE_URL + "/{petUuid}/feeding-logs";

	// AI controller
	public static final String AI_TIP_OF_THE_DAY = "/ai/tip-of-the-day";
	public static final String NUTRITION_PLAN_BASE_URL = "/ai/nutrition/plans";
	public static final String NUTRITION_PLAN_APPROVE = NUTRITION_PLAN_BASE_URL + "/{uuid}/approve";
	public static final String NUTRITION_PLAN_SEND = NUTRITION_PLAN_BASE_URL + "/{uuid}/send";
	public static final String NUTRITION_PLAN_ACTIVE = NUTRITION_PLAN_BASE_URL + "/active";

	public static final String CLINIC_BASE_URL = "/clinic";
	public static final String CLINIC_MINE = CLINIC_BASE_URL + "/mine";
	public static final String CLINIC_BY_UUID = CLINIC_BASE_URL + PATH_VARIABLE_UUID;
	public static final String CLINIC_DOCTORS = CLINIC_BY_UUID + "/doctors";
	public static final String CLINIC_PATIENTS = CLINIC_BY_UUID + "/patients";
	public static final String CLINIC_PATIENT_DETAIL = CLINIC_PATIENTS + "/{petUuid}";
	public static final String CLINIC_BOOKINGS = CLINIC_BY_UUID + "/bookings";
	public static final String CLINIC_RETENTION_ALERTS = CLINIC_BY_UUID + "/retention-alerts";
	public static final String CLINIC_RETENTION_ALERT_NOTIFY = CLINIC_RETENTION_ALERTS + "/{alertId}/notify";
	public static final String CLINIC_PATIENT_HEALTH_EVENTS = CLINIC_PATIENT_DETAIL + "/health-events";

	// Consultation invoice controller
	public static final String CONSULTATION_INVOICE_BASE_URL = "/invoice";
	public static final String CONSULTATION_INVOICE_MINE = CONSULTATION_INVOICE_BASE_URL + "/mine";
	public static final String CONSULTATION_INVOICE_BY_UUID = CONSULTATION_INVOICE_BASE_URL + PATH_VARIABLE_UUID;
	public static final String CONSULTATION_INVOICE_STATUS = CONSULTATION_INVOICE_BY_UUID + "/status";
	public static final String CONSULTATION_INVOICE_GENERATE_PDF = CONSULTATION_INVOICE_BY_UUID + "/generate-pdf";
	public static final String CONSULTATION_INVOICE_PDF = CONSULTATION_INVOICE_BY_UUID + "/pdf";
}
