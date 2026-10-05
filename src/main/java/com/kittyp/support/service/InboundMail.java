package com.kittyp.support.service;

public record InboundMail(String messageId, String from, String subject, String body, String threadId,
		String senderName) {

	public InboundMail(String messageId, String from, String subject, String body, String threadId) {
		this(messageId, from, subject, body, threadId, null);
	}
}
