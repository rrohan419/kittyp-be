package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class WhatsAppCredentialsVerifierTest {

    private MockWebServer server;
    private WhatsAppCredentialsVerifier verifier;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String base = server.url("/").toString().replaceAll("/$", "");
        verifier = new WhatsAppCredentialsVerifier(new ObjectMapper(), "v21.0", base);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void rejectsBlankToken() {
        CustomException ex = assertThrows(CustomException.class,
                () -> verifier.verifyOrThrow("", "123", "456"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void acceptsConnectedPhoneAndWaba() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"ph1\",\"status\":\"CONNECTED\",\"display_phone_number\":\"+91 98765 43210\"}")
                .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"waba1\"}")
                .addHeader("Content-Type", "application/json"));

        assertDoesNotThrow(() -> verifier.verifyOrThrow("tok", "ph1", "waba1"));

        RecordedRequest phoneReq = server.takeRequest();
        assertTrue(phoneReq.getPath().contains("/ph1"));
        assertEquals("Bearer tok", phoneReq.getHeader("Authorization"));
        RecordedRequest wabaReq = server.takeRequest();
        assertTrue(wabaReq.getPath().contains("/waba1"));
    }

    @Test
    void rejectsNonConnectedStatus() {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"ph1\",\"status\":\"PENDING\"}")
                .addHeader("Content-Type", "application/json"));

        CustomException ex = assertThrows(CustomException.class,
                () -> verifier.verifyOrThrow("tok", "ph1", "waba1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("PENDING"));
    }

    @Test
    void rejectsMetaErrorBody() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\":{\"message\":\"Invalid OAuth access token.\"}}")
                .addHeader("Content-Type", "application/json"));

        CustomException ex = assertThrows(CustomException.class,
                () -> verifier.verifyOrThrow("bad", "ph1", "waba1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("invalid oauth")
                || ex.getMessage().toLowerCase().contains("verification failed"));
    }
}
