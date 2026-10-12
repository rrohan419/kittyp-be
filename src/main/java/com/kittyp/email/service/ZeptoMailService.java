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

	void sendSignupOtpEmail(String recipientEmail, String code, String purpose, String phoneHint);

	void sendUpdateOtpEmail(String recipientEmail, String code, String name, String accountType, String newEmail);

	/**
	 * Owner consent OTP: clinic staff is adding pet {@code petName} on the owner's profile.
	 * Merge fields: Customer_Name, OTP, clinic_name, pet_name.
	 */
	void sendClinicPetConsentOtpEmail(String recipientEmail, String ownerName, String clinicName, String petName,
			String code);

	/** Parent must confirm before their KittyP account is attached as a clinic client. */
	void sendClinicClientAttachOtpEmail(String recipientEmail, String ownerName, String clinicName, String code);

	/** Invite a clinic client without a KittyP account to sign up as a pet parent. */
	void sendClinicPetParentInviteEmail(String recipientEmail, String customerName, String petName,
			String clinicName, String inviteUrl);

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

	void sendDoctorProfileRejected(String email, String doctorName, String rejectionReason);

	void sendClinicProfileVerified(String email, String customerName, String clinicName, String clinicUrl);

	void sendClinicProfileRejected(String email, String customerName, String clinicName, String rejectionReason);

	void sendInvoiceEmail(String email, String customerName, String clinicName, String petName,
			String invoiceNumber, String amount, String invoiceUrl, byte[] pdfBytes, String filename);
}
