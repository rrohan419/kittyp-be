package com.kittyp.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kittyp.contact.dto.ContactRequest;
import com.kittyp.email.dto.ZohoMailRequest;

class ContactMailTest {

	@Test
	void escapesHtmlAndAddressesSupportInbox() {
		ContactRequest request = new ContactRequest();
		request.setName("Jane <script>");
		request.setEmail("jane@example.com");
		request.setSubject("Hello & hi");
		request.setMessage("Line1\n<b>bold</b>");

		ZohoMailRequest mail = ContactMail.build("noreply@kittyp.in", "admin@kittyp.in", request);

		assertEquals("[Kittyp] Hello & hi", mail.getSubject());
		assertEquals("noreply@kittyp.in", mail.getFrom().getAddress());
		assertEquals("KittyP", mail.getFrom().getName());
		assertEquals("admin@kittyp.in", mail.getTo().get(0).getEmailAddress().getAddress());
		assertEquals("jane@example.com", mail.getReplyTo().get(0).getAddress());
		assertEquals("Jane <script>", mail.getReplyTo().get(0).getName());

		String html = mail.getHtmlBody();
		assertTrue(html.contains("Jane &lt;script&gt;"));
		assertTrue(html.contains("Hello &amp; hi"));
		assertTrue(html.contains("Line1<br>&lt;b&gt;bold&lt;/b&gt;"));
		assertFalse(html.contains("<script>"));
		assertFalse(html.contains("<b>"));
	}
}
