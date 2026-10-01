package com.kittyp.contact;

import java.util.List;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.contact.dto.ContactRequest;
import com.kittyp.email.dto.EmailAddress;
import com.kittyp.email.dto.Recipient;
import com.kittyp.email.dto.ZohoMailRequest;

/**
 * Builds the support inbox message. Visitor fields are escaped before they enter HTML.
 */
public final class ContactMail {

	private ContactMail() {
	}

	public static ZohoMailRequest build(String fromAddress, String supportEmail, ContactRequest request) {
		String name = request.getName().trim();
		String email = request.getEmail().trim();
		String subject = request.getSubject().trim();
		String message = request.getMessage().trim();

		ZohoMailRequest mail = new ZohoMailRequest();
		mail.setFrom(new EmailAddress(fromAddress, AppConstant.KITTYP));
		mail.setTo(List.of(new Recipient(new EmailAddress(supportEmail, "KittyP Support"))));
		mail.setReplyTo(List.of(new EmailAddress(email, name)));
		mail.setSubject("[Kittyp] " + stripHeader(subject));
		mail.setHtmlBody(htmlBody(name, email, subject, message));
		return mail;
	}

	static String htmlBody(String name, String email, String subject, String message) {
		String body = escape(message).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>");
		return "<p><strong>Name:</strong> " + escape(name) + "</p>"
				+ "<p><strong>Email:</strong> " + escape(email) + "</p>"
				+ "<p><strong>Subject:</strong> " + escape(subject) + "</p>"
				+ "<p>" + body + "</p>";
	}

	static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private static String stripHeader(String subject) {
		return subject.replace("\r", " ").replace("\n", " ").trim();
	}
}
