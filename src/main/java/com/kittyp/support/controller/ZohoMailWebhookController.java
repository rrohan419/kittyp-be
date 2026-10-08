package com.kittyp.support.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.support.service.SupportMailParser;
import com.kittyp.support.service.SupportMailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ZohoMailWebhookController {

	public static final String SECRET_HEADER = "X-Zoho-Webhook-Secret";

	private final ApiResponse<?> responseBuilder;
	private final SupportMailService supportMailService;
	private final SupportMailParser supportMailParser;

	@PostMapping(ApiUrl.WEBHOOK_ZOHO_MAIL)
	public ResponseEntity<SuccessResponse<String>> zohoMail(
			@RequestHeader(value = SECRET_HEADER, required = false) String secret,
			@RequestBody byte[] rawPayload) {
	
			supportMailService.verifySecret(secret);

		String payload = rawPayload == null ? null : new String(rawPayload, StandardCharsets.UTF_8);
		supportMailService.receive(supportMailParser.parse(payload));
		return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
