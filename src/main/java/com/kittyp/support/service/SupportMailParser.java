package com.kittyp.support.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

/**
 * Reads the inbound Zoho payload. Attachments are ignored. Text is capped later, not rejected.
 */
@Component
@RequiredArgsConstructor
public class SupportMailParser {

	private final ObjectMapper objectMapper;

	public InboundMail parse(String raw) {
		JsonNode root = read(raw);
		JsonNode node = unwrap(root);
		String messageId = text(node, "messageId", "message_id", "mailId", "messageID");
		String from = address(node, "fromAddress", "from", "sender", "from_address");
		String subject = text(node, "subject");
		String threadId = text(node, "threadId", "thread_id", "conversationId", "conversation_id");
		String body = text(node, "text", "content", "summary", "plainText", "body", "html");
		if (messageId == null) {
			throw new CustomException("Missing mail message id", HttpStatus.BAD_REQUEST);
		}
		if (messageId.length() > 255) {
			throw new CustomException("Mail message id is too long", HttpStatus.BAD_REQUEST);
		}
		if (from == null) {
			throw new CustomException("Missing mail sender", HttpStatus.BAD_REQUEST);
		}
		if (from.length() > 254) {
			throw new CustomException("Mail sender is too long", HttpStatus.BAD_REQUEST);
		}
		return new InboundMail(messageId, from, subject == null ? "" : subject, body == null ? "" : body, threadId);
	}

	private JsonNode read(String raw) {
		try {
			return objectMapper.readTree(raw);
		} catch (Exception ex) {
			throw new CustomException("Invalid mail payload", HttpStatus.BAD_REQUEST);
		}
	}

	private static JsonNode unwrap(JsonNode root) {
		if (root != null && root.has("data") && root.get("data").isObject()) {
			return root.get("data");
		}
		if (root != null && root.has("mail") && root.get("mail").isObject()) {
			return root.get("mail");
		}
		return root;
	}

	private static String address(JsonNode node, String... names) {
		for (String name : names) {
			if (node == null || !node.has(name) || node.get(name).isNull()) {
				continue;
			}
			JsonNode value = node.get(name);
			if (value.isObject()) {
				String nested = text(value, "address", "email", "emailAddress");
				if (nested != null) {
					return emailOnly(nested);
				}
			} else {
				String textual = scalar(value);
				if (textual != null) {
					return emailOnly(textual);
				}
			}
		}
		return null;
	}

	private static String text(JsonNode node, String... names) {
		for (String name : names) {
			if (node == null || !node.has(name)) {
				continue;
			}
			String textual = scalar(node.get(name));
			if (textual != null) {
				return textual;
			}
		}
		return null;
	}

	private static String scalar(JsonNode value) {
		if (value == null || value.isNull()) {
			return null;
		}
		if (value.isTextual() || value.isNumber()) {
			String text = value.asText().trim();
			return text.isEmpty() ? null : text;
		}
		return null;
	}

	private static String emailOnly(String raw) {
		String trimmed = raw.trim();
		int start = trimmed.lastIndexOf('<');
		int end = trimmed.lastIndexOf('>');
		if (start >= 0 && end > start) {
			trimmed = trimmed.substring(start + 1, end).trim();
		}
		return trimmed.isEmpty() ? null : trimmed;
	}
}
