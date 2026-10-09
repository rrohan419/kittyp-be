package com.kittyp.contact.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kittyp.common.exception.CustomException;

@Component
public class ContactRateLimiter {

	private final int maxPerMinute;

	private final Cache<String, AtomicInteger> window = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(1))
			.build();

	public ContactRateLimiter(@Value("${contact.rate-limit.per-minute}") int maxPerMinute) {
		this.maxPerMinute = maxPerMinute;
	}

	public void check(String clientKey) {
		String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
		AtomicInteger count = window.get(key, ignored -> new AtomicInteger(0));
		if (count != null && count.incrementAndGet() > maxPerMinute) {
			throw new CustomException("Too many messages. Please try again in a minute.", HttpStatus.TOO_MANY_REQUESTS);
		}
	}
}
