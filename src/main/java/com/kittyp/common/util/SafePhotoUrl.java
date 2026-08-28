package com.kittyp.common.util;

import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

public final class SafePhotoUrl {

	private static final int MAX_LEN = 2048;

	private SafePhotoUrl() {
	}

	/** Blank allowed. Otherwise http(s) URL with no quotes, angles, backslash, or whitespace. */
	public static String requireHttps(String url) {
		if (url == null || url.isBlank()) {
			return url;
		}
		String value = url.trim();
		if (value.length() > MAX_LEN) {
			throw new CustomException("Invalid photo URL", HttpStatus.BAD_REQUEST);
		}
		String lower = value.toLowerCase();
		if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:")
				|| !(lower.startsWith("https://") || lower.startsWith("http://"))) {
			throw new CustomException("Invalid photo URL", HttpStatus.BAD_REQUEST);
		}
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c <= 0x20 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '\\') {
				throw new CustomException("Invalid photo URL", HttpStatus.BAD_REQUEST);
			}
		}
		return value;
	}
}
