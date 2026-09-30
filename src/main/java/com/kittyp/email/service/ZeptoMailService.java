/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

/**
 * @author rrohan419@gmail.com 
 */
public interface ZeptoMailService {

	void sendWelcomeEmailforParent(String firstName, String recipientEmail);

	void sendWelcomeEmailforDoctor(String recipientEmail);

	void sendWelcomeEmailforClinicAdmin(String recipientEmail);
	
	void sendPasswordResetCode(String email);

	void sendOrderConfirmationEmail(String recipientEmail, String orderNumber);

	/** Sends a signup OTP email. purpose e.g. "email" or "phone". */
	void sendSignupOtpEmail(String recipientEmail, String code, String purpose, String phoneHint);

	/**
	 * Owner consent OTP: clinic staff is adding pet {@code petName} on the owner's profile.
	 * Merge fields: Customer_Name, OTP, clinic_name, pet_name.
	 */
	void sendClinicPetConsentOtpEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String code);

	/** Parent must confirm before their KittyP account is attached as a clinic client. */
	void sendClinicClientAttachOtpEmail(String recipientEmail, String ownerName, String clinicName, String code);

	/** Clinic doctor invitation with accept link. */
	void sendClinicDoctorInviteEmail(String recipientEmail, String doctorName, String clinicName, String acceptUrl);

	/** Clinic staff invitation with complete-signup link. */
	void sendClinicStaffInviteEmail(String recipientEmail, String staffName, String clinicName, String acceptUrl);

	/** Reminder for a pending clinic doctor invite. */
	void sendClinicDoctorInviteReminderEmail(String recipientEmail, String doctorName, String clinicName,
			String acceptUrl);

	/** Notify clinic that a doctor accepted or declined an invite. */
	void sendClinicDoctorInviteResponseEmail(String recipientEmail, String clinicName, String doctorName,
			String doctorEmail, boolean accepted);

	void sendDoctorProfileVerified(String email, String doctorName, String dashboardUrl);

	void sendClinicProfileVerified(String email, String customerName, String clinicName, String clinicUrl);

	void sendInvoiceEmail(String email, String customerName, String clinicName, String petName,
			String invoiceNumber, String amount, String invoiceUrl, byte[] pdfBytes, String filename);

	void sendPhoneChangedNotification(String email, String firstName, String clinicName, String phone, String loginUrl);

	void sendEmailChangeOtp(String recipientEmail, String firstName, String clinicName, String code);

	void sendPasswordChangedNotification(String email, String firstName, String clinicName, String changedAt);

	/** Welcome for clinic CRM parent without a KittyP account yet. */
	void sendClinicParentCrmWelcomeEmail(String firstName, String recipientEmail, String clinicName);

	/** Confirm appointment/walk-in to the pet owner email. Skips when the dedicated template id is blank. */
	void sendAppointmentConfirmationEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicAddress, String mapsUrl,
			String rescheduleUrl, String cancelUrl);

	/**
	 * One-hour pre-visit reminder. Runs on the caller thread and returns false when the mail is not accepted.
	 */
	boolean sendAppointmentReminderEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicPhone, String clinicAddress, String mapsUrl);

	/** Sent after the slot changes. Dedicated template; does not reuse welcome. */
	void sendAppointmentRescheduledEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicAddress, String mapsUrl,
			String previousWhen, String clinicPhone, String manageUrl);

	/** Sent to the pet owner after a successful cancel. Dedicated template. */
	void sendAppointmentCancelledEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicPhone, String clinicAddress, String bookUrl);

	/** Walk-in check-in. No reschedule or cancel link. */
	void sendWalkInCheckedInEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String doctorName, String visitId, String clinicPhone, String clinicAddress, String mapsUrl);

	/** Same slot, different doctor. */
	void sendAppointmentDoctorChangedEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String doctorUuid, String clinicAddress, String mapsUrl,
			String clinicPhone, String manageUrl);

	/** Upcoming visit cancelled because the clinic shut down. */
	void sendClinicClosureEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String when, String doctorName, String bookingId, String clinicPhone);

	/** Doctor or staff invite was withdrawn. inviteRole is "doctor" or "staff". */
	void sendInviteRevokedEmail(String recipientEmail, String inviteeName, String clinicName, String inviteRole,
			String inviteUuid);

	/** Clinic address changed. moveDate is the display date; eventDate is yyyy-MM-dd. */
	void sendClinicLocationChangedEmail(String recipientEmail, String recipientName, String clinicName,
			String moveDate, String oldLocation, String newLocation, String oldMapsUrl, String newMapsUrl,
			String clinicUuid, String eventDate);

	/** Welcome after clinic staff completes invite signup. */
	void sendWelcomeEmailforClinicStaff(String firstName, String recipientEmail, String clinicName);
}
