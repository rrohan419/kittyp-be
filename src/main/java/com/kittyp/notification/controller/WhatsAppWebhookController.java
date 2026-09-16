package com.kittyp.notification.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(ApiUrl.BASE_URL)
public class WhatsAppWebhookController {

    private final ObjectMapper objectMapper;
    private final String verifyToken;
    private final String appSecret;

    public WhatsAppWebhookController(
            ObjectMapper objectMapper,
            @Value("${whatsapp.webhook-verify-token:}") String verifyToken,
            @Value("${whatsapp.meta.app-secret:}") String appSecret) {
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken == null ? "" : verifyToken.trim();
        this.appSecret = appSecret == null ? "" : appSecret.trim();
    }

    @GetMapping(ApiUrl.WHATSAPP_WEBHOOK)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && StringUtils.hasText(verifyToken) && verifyToken.equals(token)
                && StringUtils.hasText(challenge)) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Forbidden");
    }

    @PostMapping(ApiUrl.WHATSAPP_WEBHOOK)
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) String rawBody) {
        verifySignature(rawBody, signature);
        try {
            JsonNode root = objectMapper.readTree(rawBody == null || rawBody.isBlank() ? "{}" : rawBody);
            JsonNode messages = root.path("entry").path(0).path("changes").path(0).path("value").path("messages");
            if (messages.isArray()) {
                for (JsonNode msg : messages) {
                    String from = msg.path("from").asText("");
                    String id = msg.path("id").asText("");
                    String type = msg.path("type").asText("");
                    String text = msg.path("text").path("body").asText("");
                    if (text.length() > 80) {
                        text = text.substring(0, 80) + "…";
                    }
                    log.info("WhatsApp inbound id={} type={} from={} text={}", id, type, from, text);
                }
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WhatsApp webhook payload parse skipped: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private void verifySignature(String rawBody, String signatureHeader) {
        if (!StringUtils.hasText(appSecret)) {
            throw new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith("sha256=")) {
            throw new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
        }
        String expected = "sha256=" + hmacSha256Hex(appSecret, rawBody == null ? "" : rawBody);
        byte[] left = expected.getBytes(StandardCharsets.US_ASCII);
        byte[] right = signatureHeader.trim().getBytes(StandardCharsets.US_ASCII);
        if (left.length != right.length || !MessageDigest.isEqual(left, right)) {
            throw new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
        }
    }

    private static String hmacSha256Hex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new CustomException("Could not verify webhook signature", HttpStatus.BAD_REQUEST, e);
        }
    }
}
