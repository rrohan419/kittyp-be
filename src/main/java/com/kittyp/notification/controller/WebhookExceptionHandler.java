package com.kittyp.notification.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

/**
 * Keeps Meta webhook errors as raw text/plain. Does not use the global JSON envelope.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = WhatsAppWebhookController.class)
public class WebhookExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustom(CustomException ex) {
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_REQUEST;
        if (status != HttpStatus.FORBIDDEN && status != HttpStatus.BAD_REQUEST) {
            status = status.is4xxClientError() ? status : HttpStatus.BAD_REQUEST;
        }
        return raw(status, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAny(Exception ex) {
        log.warn("WhatsApp webhook error: {}", ex.getMessage());
        return raw(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Bad request");
    }

    private static ResponseEntity<String> raw(HttpStatus status, String body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body == null || body.isBlank() ? status.getReasonPhrase() : body);
    }
}
