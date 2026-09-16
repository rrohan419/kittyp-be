package com.kittyp.notification.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

class WhatsAppWebhookControllerTest {

    private static final String APP_SECRET = "meta-app-secret";

    private WhatsAppWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WhatsAppWebhookController(new ObjectMapper(), "verify-me", APP_SECRET);
    }

    @Test
    void verify_matchingToken_returnsRawChallenge() {
        ResponseEntity<String> res = controller.verify("subscribe", "verify-me", "12345");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, res.getHeaders().getContentType());
        assertEquals("12345", res.getBody());
    }

    @Test
    void verify_mismatch_returns403TextPlain() {
        ResponseEntity<String> res = controller.verify("subscribe", "wrong", "12345");
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, res.getHeaders().getContentType());
        assertEquals("Forbidden", res.getBody());
    }

    @Test
    void receive_messagesPayload_returns200() {
        String body = """
                {"entry":[{"changes":[{"value":{"messages":[{
                  "id":"wamid.1","from":"919876543210","type":"text",
                  "text":{"body":"hello from parent"}
                }]}}]}]}
                """;
        ResponseEntity<Void> res = controller.receive(hmac(APP_SECRET, body), body);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNull(res.getBody());
    }

    @Test
    void receive_blankSecret_unauthorized() {
        WhatsAppWebhookController blank = new WhatsAppWebhookController(new ObjectMapper(), "verify-me", "");
        CustomException ex = assertThrows(CustomException.class, () -> blank.receive("sha256=abc", "{}"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void webhookAdvice_unauthorizedStaysTextPlain401() {
        WebhookExceptionHandler advice = new WebhookExceptionHandler();
        ResponseEntity<String> res = advice.handleCustom(
                new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED));
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, res.getHeaders().getContentType());
        assertEquals("Invalid webhook signature", res.getBody());
    }

    @Test
    void webhookAdvice_returnsTextPlain() {
        WebhookExceptionHandler advice = new WebhookExceptionHandler();
        ResponseEntity<String> res = advice.handleAny(new IllegalStateException("boom"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, res.getHeaders().getContentType());
        assertTrue(res.getBody().contains("boom"));
    }

    static String hmac(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
