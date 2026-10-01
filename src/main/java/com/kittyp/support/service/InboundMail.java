package com.kittyp.support.service;

public record InboundMail(String messageId, String from, String subject, String body, String threadId) {
}
