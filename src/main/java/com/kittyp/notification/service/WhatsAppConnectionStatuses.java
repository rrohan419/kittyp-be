package com.kittyp.notification.service;

/**
 * Cached WhatsApp connection / template status values stored on clinic and doctor profile.
 */
public final class WhatsAppConnectionStatuses {

	public static final String DISCONNECTED = "DISCONNECTED";
	public static final String CONNECTED = "CONNECTED";
	public static final String ERROR = "ERROR";

	public static final String TEMPLATE_MISSING = "MISSING";
	public static final String TEMPLATE_PENDING = "PENDING";
	public static final String TEMPLATE_APPROVED = "APPROVED";
	public static final String TEMPLATE_REJECTED = "REJECTED";
	public static final String TEMPLATE_ERROR = "ERROR";

	private WhatsAppConnectionStatuses() {
	}

	public static boolean isInvoiceTemplateApproved(String status) {
		return TEMPLATE_APPROVED.equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status);
	}

	public static String normalizeTemplateStatus(String raw) {
		if (raw == null || raw.isBlank()) {
			return TEMPLATE_MISSING;
		}
		String s = raw.trim().toUpperCase();
		if ("ACTIVE".equals(s)) {
			return TEMPLATE_APPROVED;
		}
		if ("IN_APPEAL".equals(s)) {
			return TEMPLATE_PENDING;
		}
		return s;
	}
}
