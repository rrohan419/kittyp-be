package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

class SafePhotoUrlTest {

	@Test
	void allowsBlankAndHttps() {
		assertNull(SafePhotoUrl.requireHttps(null));
		assertEquals("", SafePhotoUrl.requireHttps(""));
		assertEquals("https://cdn.example/pet.jpg", SafePhotoUrl.requireHttps("https://cdn.example/pet.jpg"));
		assertEquals("http://cdn.example/pet.jpg", SafePhotoUrl.requireHttps("http://cdn.example/pet.jpg"));
	}

	@Test
	void rejectsInjectionLikeUrls() {
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(CustomException.class,
				() -> SafePhotoUrl.requireHttps("javascript:alert(1)")).getHttpStatus());
		assertThrows(CustomException.class, () -> SafePhotoUrl.requireHttps("ftp://cdn.example/pet.jpg"));
		assertThrows(CustomException.class, () -> SafePhotoUrl.requireHttps("https://cdn.example/pet.jpg\"onclick=x"));
		assertThrows(CustomException.class, () -> SafePhotoUrl.requireHttps("https://cdn.example/pet.jpg' OR 1=1 --"));
		assertThrows(CustomException.class, () -> SafePhotoUrl.requireHttps("https://cdn.example/<script>.jpg"));
	}
}
