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

	public static final String ADMIN_SYSTEM_HEALTH = ADMIN + "/system-health";
	public static final String ADMIN_SYSTEM_HEALTH_OPTIMIZE = ADMIN_SYSTEM_HEALTH + "/optimize";
	public static final String ADMIN_SYSTEM_HEALTH_LOAD_START = ADMIN_SYSTEM_HEALTH + "/load-test/start";
	public static final String ADMIN_SYSTEM_HEALTH_LOAD_STOP = ADMIN_SYSTEM_HEALTH + "/load-test/stop";
	public static final String ADMIN_DOCTORS = ADMIN + "/doctors";
	public static final String ADMIN_DOCTOR_BY_UUID = ADMIN_DOCTORS + PATH_VARIABLE_UUID;
	public static final String ADMIN_DOCTOR_STATUS = ADMIN_DOCTOR_BY_UUID + "/status";
	public static final String ADMIN_DOCTOR_CHECKLIST = ADMIN_DOCTOR_BY_UUID + "/checklist";
	public static final String ADMIN_CLINICS = ADMIN + "/clinics";
	public static final String ADMIN_CLINIC_BY_UUID = ADMIN_CLINICS + PATH_VARIABLE_UUID;
	public static final String ADMIN_CLINIC_STATUS = ADMIN_CLINIC_BY_UUID + "/status";
	public static final String ADMIN_USERS = ADMIN + "/users";
	public static final String ADMIN_PARENTS = ADMIN + "/parents";

	public static final String DOCTOR_BASE_URL = "/doctor";
	public static final String DOCTOR_ME = DOCTOR_BASE_URL + "/me";
	public static final String DOCTOR_ME_AVAILABILITY = DOCTOR_ME + "/availability";

	public static final String UPLOAD_SIGNUP_DOCUMENTS = "/upload/signup-documents";
	public static final String UPLOAD_CLINICAL = "/upload/clinical";

	// User controller
	public static final String USER_BASE_URL = "/user";
	public static final String USER_DETAILS = USER_BASE_URL + "/me";
	public static final String USER_CLINICS = USER_BASE_URL + "/clinics";
	public static final String USER_SWITCH_CLINIC = USER_BASE_URL + "/switch-clinic";
	public static final String USER_ADDRESS = USER_BASE_URL + "/address";
	public static final String USER_ADDRESS_DETAIL = USER_BASE_URL + "/address/detail";
	public static final String USER_PROFILE_OTP_SEND = USER_BASE_URL + "/otp/send";
	public static final String USER_PROFILE_OTP_VERIFY = USER_BASE_URL + "/otp/verify";

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
	public static final String AUTHOR_ME = ARTICLE_BASE_URL + AUTHOR_BASE_URL + "/me";

	/** Alias surface for Loop 2 doctor blogs (delegates to article module). */
	public static final String BLOGS_BASE_URL = "/blogs";
	public static final String BLOGS_ALL = BLOGS_BASE_URL + "/all";
	public static final String BLOGS_BY_SLUG = BLOGS_BASE_URL + "/{slug}";

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
	public static final String NUTRITION_PLAN_UPDATE = NUTRITION_PLAN_BASE_URL + "/{uuid}";
	public static final String NUTRITION_PLAN_ACTIVE = NUTRITION_PLAN_BASE_URL + "/active";

	public static final String CLINIC_BASE_URL = "/clinic";
	public static final String CLINIC_MINE = CLINIC_BASE_URL + "/mine";
	public static final String CLINIC_BY_UUID = CLINIC_BASE_URL + PATH_VARIABLE_UUID;
	public static final String CLINIC_DOCTORS = CLINIC_BY_UUID + "/doctors";
	public static final String CLINIC_DOCTOR_BY_UUID = CLINIC_DOCTORS + "/{doctorUuid}";
	public static final String CLINIC_DOCTOR_INVITE = CLINIC_DOCTORS + "/invite";
	public static final String CLINIC_DOCTOR_INVITES = CLINIC_DOCTORS + "/invites";
	public static final String CLINIC_DOCTOR_INVITE_REVOKE = CLINIC_DOCTOR_INVITES + "/{inviteUuid}/revoke";
	public static final String CLINIC_DOCTOR_INVITE_REMIND = CLINIC_DOCTOR_INVITES + "/{inviteUuid}/remind";
	public static final String CLINIC_DOCTOR_LOOKUP = CLINIC_BASE_URL + "/doctors/lookup";
	public static final String CLINIC_MY_INVITES = CLINIC_BASE_URL + "/my-doctor-invites";
	public static final String CLINIC_INVITE_BY_TOKEN = CLINIC_BASE_URL + "/invites/{token}";
	public static final String CLINIC_INVITE_ACCEPT = CLINIC_INVITE_BY_TOKEN + "/accept";
	public static final String CLINIC_INVITE_REJECT = CLINIC_INVITE_BY_TOKEN + "/reject";
	public static final String CLINIC_STAFF = CLINIC_BY_UUID + "/staff";
	public static final String CLINIC_STAFF_INVITE = CLINIC_STAFF + "/invite";
	public static final String CLINIC_STAFF_INVITES = CLINIC_STAFF + "/invites";
	public static final String CLINIC_STAFF_DISABLE = CLINIC_STAFF + "/{userUuid}/disable";
	public static final String CLINIC_STAFF_INVITE_REVOKE = CLINIC_STAFF_INVITES + "/{inviteUuid}/revoke";
	public static final String CLINIC_STAFF_INVITE_BY_TOKEN = CLINIC_BASE_URL + "/staff-invite/{token}";
	public static final String CLINIC_STAFF_INVITE_COMPLETE = CLINIC_STAFF_INVITE_BY_TOKEN + "/complete";
	public static final String CLINIC_PATIENTS = CLINIC_BY_UUID + "/patients";
	public static final String CLINIC_PATIENT_DETAIL = CLINIC_PATIENTS + "/{petUuid}";
	public static final String CLINIC_PATIENT_ADD = CLINIC_PATIENTS;
	public static final String CLINIC_OWNERS = CLINIC_BY_UUID + "/owners";
	public static final String CLINIC_OWNER_DETAIL = CLINIC_OWNERS + "/{ownerUuid}";
	public static final String CLINIC_OWNER_PETS = CLINIC_OWNER_DETAIL + "/pets";
	public static final String CLINIC_OWNER_HIDE = CLINIC_OWNER_DETAIL + "/hide";
	public static final String CLINIC_OWNER_FROM_USER = CLINIC_OWNERS + "/from-user";
	public static final String CLINIC_OWNER_LOOKUP = CLINIC_OWNERS + "/lookup";
	public static final String CLINIC_OWNER_PET_CONSENT_SEND = CLINIC_OWNER_DETAIL + "/pets/consent/send";
	public static final String CLINIC_OWNER_PET_CONSENT_VERIFY = CLINIC_OWNER_DETAIL + "/pets/consent/verify";
	public static final String CLINIC_USERS_SEARCH = CLINIC_BY_UUID + "/users/search";
	public static final String CLINIC_PETS = CLINIC_BY_UUID + "/pets";
	public static final String CLINIC_PET_DETAIL = CLINIC_PETS + "/{petUuid}";
	public static final String CLINIC_PET_ADMIT = CLINIC_PET_DETAIL + "/admit";
	public static final String CLINIC_PET_HIDE = CLINIC_PET_DETAIL + "/hide";
	public static final String CLINIC_PET_RECORDS = CLINIC_PET_DETAIL + "/records";
	public static final String CLINIC_PET_VACCINES = CLINIC_PET_DETAIL + "/vaccines";
	public static final String CLINIC_PET_VACCINE = CLINIC_PET_VACCINES + "/{scheduleId}";
	public static final String CLINIC_VACCINE_CATALOG = CLINIC_BY_UUID + "/vaccine-catalog";
	public static final String CLINIC_BOOKINGS = CLINIC_BY_UUID + "/bookings";
	public static final String CLINIC_BOOKING_BY_UUID = CLINIC_BOOKINGS + "/{bookingUuid}";
	public static final String CLINIC_VISITS = CLINIC_BY_UUID + "/visits";
	public static final String CLINIC_VISITS_WALK_IN = CLINIC_VISITS + "/walk-in";
	public static final String CLINIC_VISIT_BY_UUID = CLINIC_VISITS + "/{visitUuid}";
	public static final String CLINIC_PATIENT_VISITS = CLINIC_PATIENT_DETAIL + "/visits";
	public static final String CLINIC_RETENTION_ALERTS = CLINIC_BY_UUID + "/retention-alerts";
	public static final String CLINIC_RETENTION_ALERT_NOTIFY = CLINIC_RETENTION_ALERTS + "/{alertId}/notify";
	public static final String CLINIC_PATIENT_HEALTH_EVENTS = CLINIC_PATIENT_DETAIL + "/health-events";
	public static final String CLINIC_SHUTDOWN = CLINIC_BY_UUID + "/shutdown";
	public static final String CLINIC_REOPEN = CLINIC_BY_UUID + "/reopen";
	public static final String CLINIC_STATS = CLINIC_BY_UUID + "/stats";
	public static final String CLINIC_INVOICES = CLINIC_BY_UUID + "/invoices";
	public static final String CLINIC_INVOICE_BY_UUID = CLINIC_INVOICES + "/{invoiceUuid}";
	public static final String CLINIC_INVOICE_GENERATE_PDF = CLINIC_INVOICE_BY_UUID + "/generate-pdf";
	public static final String CLINIC_INVOICE_PDF = CLINIC_INVOICE_BY_UUID + "/pdf";
	public static final String CLINIC_INVOICE_SEND_WHATSAPP = CLINIC_INVOICE_BY_UUID + "/send-whatsapp";
	public static final String CLINIC_INVOICE_MARK_PAID = CLINIC_INVOICE_BY_UUID + "/mark-paid";
	public static final String CLINIC_WHATSAPP_SETTINGS = CLINIC_BY_UUID + "/whatsapp-settings";
	public static final String DOCTOR_WHATSAPP_SETTINGS = "/doctor/whatsapp-settings";

	public static final String DOCTOR_VISITS = DOCTOR_BASE_URL + "/visits";
	public static final String DOCTOR_VISITS_MINE = DOCTOR_VISITS + "/mine";
	public static final String DOCTOR_VISIT_BY_UUID = DOCTOR_VISITS + "/{visitUuid}";
	public static final String DOCTOR_VISIT_START = DOCTOR_VISIT_BY_UUID + "/start";
	public static final String DOCTOR_VISIT_CHART = DOCTOR_VISIT_BY_UUID + "/chart";
	public static final String DOCTOR_VISIT_COMPLETE = DOCTOR_VISIT_BY_UUID + "/complete";
	public static final String DOCTOR_VISIT_RETURN = DOCTOR_VISIT_BY_UUID + "/return-to-reception";
	public static final String DOCTOR_ATTENDED_PATIENTS = DOCTOR_BASE_URL + "/patients/attended";
	public static final String DOCTOR_BOOKINGS_MINE = DOCTOR_BASE_URL + "/bookings/mine";
	public static final String DOCTOR_BOOKING_BY_UUID = DOCTOR_BASE_URL + "/bookings/{bookingUuid}";
	public static final String DOCTOR_BOOKING_START_TREATMENT = DOCTOR_BOOKING_BY_UUID + "/start-treatment";
	public static final String DOCTOR_BOOKING_VIDEO = DOCTOR_BOOKING_BY_UUID + "/video";

	public static final String CLINIC_DOCTOR_BUSY = CLINIC_DOCTOR_BY_UUID + "/busy";

	public static final String PET_VISITS = PET_BY_UUID + "/visits";
	public static final String PET_INVOICES = PET_BY_UUID + "/invoices";
	public static final String PET_INVOICE_BY_UUID = PET_INVOICES + "/{invoiceUuid}";
	public static final String PET_INVOICE_PDF = PET_INVOICE_BY_UUID + "/pdf";
	public static final String USER_VISITS_MINE = "/user/visits/mine";
	public static final String USER_VISIT_BY_UUID = "/user/visits/{visitUuid}";
	public static final String USER_VISIT_RATING = USER_VISIT_BY_UUID + "/rating";
	public static final String USER_BOOKINGS_MINE = "/user/bookings/mine";
	public static final String USER_BOOKINGS = "/user/bookings";
	public static final String USER_BOOKING_VIDEO = USER_BOOKINGS + "/{bookingUuid}/video";
	public static final String USER_DOCTOR_SLOTS = "/user/clinics/{clinicUuid}/doctors/{doctorUuid}/slots";
	public static final String USER_REMINDERS = "/user/reminders";
	public static final String USER_REMINDER_BY_UUID = USER_REMINDERS + "/{reminderUuid}";

	public static final String DISCOVER_BASE = "/discover";
	public static final String DISCOVER_CLINICS = DISCOVER_BASE + "/clinics";
	public static final String DISCOVER_DOCTORS = DISCOVER_BASE + "/doctors";
	public static final String DISCOVER_CLINIC_DOCTORS = DISCOVER_CLINICS + "/{clinicUuid}/doctors";

	// Consultation invoice controller
	public static final String CONSULTATION_INVOICE_BASE_URL = "/invoice";
	public static final String CONSULTATION_INVOICE_MINE = CONSULTATION_INVOICE_BASE_URL + "/mine";
	public static final String CONSULTATION_INVOICE_BY_UUID = CONSULTATION_INVOICE_BASE_URL + PATH_VARIABLE_UUID;
	public static final String CONSULTATION_INVOICE_STATUS = CONSULTATION_INVOICE_BY_UUID + "/status";
	public static final String CONSULTATION_INVOICE_GENERATE_PDF = CONSULTATION_INVOICE_BY_UUID + "/generate-pdf";
	public static final String CONSULTATION_INVOICE_PDF = CONSULTATION_INVOICE_BY_UUID + "/pdf";
	public static final String CONSULTATION_INVOICE_SEND_WHATSAPP = CONSULTATION_INVOICE_BY_UUID + "/send-whatsapp";
	public static final String CONSULTATION_INVOICE_MARK_PAID = CONSULTATION_INVOICE_BY_UUID + "/mark-paid";
}
