/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.constants;

/**
 * @author rrohan419@gmail.com 
 */
public class TemplateConstant {

	private TemplateConstant() {}
	
	// public static final String ZEPTO_WELCOME_EMAIL_TEMPLATE_ID = "2518b.3393a5309f0a2602.k1.12930630-31f1-11f0-950e-8e9a6c33ddc2.196d69ace13";
	public static final String ZOHO_PARENT_WELCOME_EMAIL_TEMPLATE_ID="zoho.parent.welcome.template.id";
	public static final String ZOHO_DOCTOR_WELCOME_EMAIL_TEMPLATE_ID="zoho.doctor.welcome.template.id";
	public static final String ZOHO_CLINIC_ADMIN_WELCOME_EMAIL_TEMPLATE_ID="zoho.clinic.admin.welcome.template.id";
	public static final String ZEPTO_SIGNUP_OTP_EMAIL_TEMPLATE_ID = "zoho.signup.email.otp.template.id";
	/** Owner consent OTP when clinic staff adds a pet on their behalf (reuses signup OTP template by default). */
	public static final String ZEPTO_CLINIC_PET_CONSENT_OTP_EMAIL_TEMPLATE_ID = "zoho.clinic.pet.consent.otp.email.template.id";
	public static final String ZEPTO_CLINIC_DOCTOR_INVITE_EMAIL_TEMPLATE_ID = "zoho.clinic.doctor.invite.email.template.id";
	public static final String ZEPTO_CLINIC_STAFF_INVITE_EMAIL_TEMPLATE_ID = "zoho.clinic.staff.invite.email.template.id";
	public static final String ZEPTO_CLINIC_DOCTOR_INVITE_RESPONSE_EMAIL_TEMPLATE_ID = "zoho.clinic.doctor.invite.response.email.template.id";
	public static final String ZEPTO_RESET_PASSWORD_CODE_EMAIL_TEMPLATE_ID = "zoho.password.reset.code.email.template.id";
	public static final String ZEPTO_ORDER_CONFIRMATION_EMAIL_TEMPLATE_ID = "zoho.order.confirmation.email.template.id";
	public static final String ZEPTO_CLINIC_CLIENT_CONSENT_EMAIL_TEMPLATE_ID = "zoho.clinic.client.consent.email.template.id";
	public static final String ZOHO_DOCTOR_PROFILE_VERIFIED_TEMPLATE_ID = "zoho.doctor.profile.verified.template.id";
	public static final String ZOHO_CLINIC_PROFILE_VERIFIED_TEMPLATE_ID = "zoho.clinic.profile.verified.template.id";
	public static final String ZOHO_TREATMENT_INVOICE_EMAIL_TEMPLATE_ID = "zoho.treatment.invoice.email.template.id";
	/** CRM parent (no KittyP account yet) welcome after walk-in / schedule. */
	public static final String ZEPTO_CLINIC_PARENT_CRM_WELCOME_EMAIL_TEMPLATE_ID =
			"zoho.clinic.parent.crm.welcome.email.template.id";
	/** Owner appointment confirmation (walk-in or scheduled). */
	public static final String ZEPTO_APPOINTMENT_CONFIRMATION_EMAIL_TEMPLATE_ID =
			"zoho.appointment.confirmation.email.template.id";
	/** Staff account created after invite accept. */
	public static final String ZEPTO_CLINIC_STAFF_WELCOME_EMAIL_TEMPLATE_ID =
			"zoho.clinic.staff.welcome.email.template.id";
	public static final String ZOHO_ACCOUNT_PHONE_CHANGED_TEMPLATE_ID = "zoho.phone.changed.email.template.id";
	public static final String ZOHO_ACCOUNT_PASSWORD_CHANGED_TEMPLATE_ID = "zoho.password.changed.email.template.id";
	public static final String ZOHO_EMAIL_CHANGE_OTP_TEMPLATE_ID = "zoho.email.change.otp.template.id";
	public static final String ZEPTO_CLINIC_DOCTOR_INVITE_REMINDER_EMAIL_TEMPLATE_ID =
			"zoho.clinic.doctor.invite.reminder.email.template.id";
	public static final String ZEPTO_PHONE_CHANGED_EMAIL_TEMPLATE_ID = ZOHO_ACCOUNT_PHONE_CHANGED_TEMPLATE_ID;
	public static final String ZEPTO_EMAIL_CHANGE_OTP_TEMPLATE_ID = ZOHO_EMAIL_CHANGE_OTP_TEMPLATE_ID;
	public static final String ZEPTO_PASSWORD_CHANGED_EMAIL_TEMPLATE_ID = ZOHO_ACCOUNT_PASSWORD_CHANGED_TEMPLATE_ID;
}
