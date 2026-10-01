package com.kittyp.support.service;

import java.util.List;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.email.dto.EmailAddress;
import com.kittyp.email.dto.Recipient;
import com.kittyp.email.dto.ZohoMailRequest;

/**
 * One-time acknowledgement for a new support conversation. Uses raw HTML, not the template sender.
 */
public final class SupportAckMail {

	private SupportAckMail() {
	}

	public static ZohoMailRequest build(String fromAddress, String supportInbox, String recipient, String supportId) {
		ZohoMailRequest mail = new ZohoMailRequest();
		mail.setFrom(new EmailAddress(fromAddress, AppConstant.KITTYP));
		mail.setTo(List.of(new Recipient(new EmailAddress(recipient, recipient))));
		mail.setSubject("[KittyP #" + supportId + "] We received your request");
		mail.setClientReference(supportId);
		mail.setHtmlBody(htmlBody(supportInbox, supportId));
		return mail;
	}

	static String htmlBody(String supportInbox, String supportId) {
		return "<p>Hi,</p>"
				+ "<p>Thank you for contacting KittyP Care.</p>"
				+ "<p>We have received your support request.</p>"
				+ "<p>Support ID:<br>" + escape(supportId) + "</p>"
				+ "<p>Our team will review your request and get back to you shortly.</p>"
				+ "<p>Please keep this Support ID for future communication.</p>"
				+ "<p>Regards,<br>KittyP Care Support<br>" + escape(supportInbox) + "</p>";
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
