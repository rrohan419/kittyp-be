package com.kittyp.places.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kittyp.common.exception.CustomException;

@Component
public class PlacesRateLimiter {

	private static final int PUBLIC_MAX_PER_MINUTE = 30;

	private final Cache<String, AtomicInteger> window = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(1))
			.build();

	public void checkPublic(String clientKey) {
		String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
		AtomicInteger count = window.get(key, ignored -> new AtomicInteger(0));
		if (count != null && count.incrementAndGet() > PUBLIC_MAX_PER_MINUTE) {
			throw new CustomException("Too many place searches", HttpStatus.TOO_MANY_REQUESTS);
		}
	}
}
