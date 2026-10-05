package com.kittyp.support.controller;

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
	public static final String HOOK_SECRET_HEADER = "X-Hook-Secret";
	public static final String HOOK_SIGNATURE_HEADER = "X-Hook-Signature";

	private final ApiResponse<?> responseBuilder;
	private final SupportMailService supportMailService;
	private final SupportMailParser supportMailParser;

	@PostMapping(ApiUrl.WEBHOOK_ZOHO_MAIL)
	public ResponseEntity<SuccessResponse<String>> zohoMail(
			@RequestHeader(value = SECRET_HEADER, required = false) String secret,
			@RequestHeader(value = HOOK_SECRET_HEADER, required = false) String hookSecret,
			@RequestHeader(value = HOOK_SIGNATURE_HEADER, required = false) String hookSignature,
			@RequestBody String rawPayload) {
		if (hasText(hookSecret)) {
			supportMailService.acceptHookSecret(hookSecret);
			ResponseEntity<SuccessResponse<String>> accepted = responseBuilder.buildSuccessResponse(null,
					ResponseMessage.SUCCESS, HttpStatus.OK);
			return ResponseEntity.ok().header(HOOK_SECRET_HEADER, hookSecret).body(accepted.getBody());
		}
		if (hasText(hookSignature)) {
			supportMailService.verifyHookSignature(rawPayload, hookSignature);
		} else {
			supportMailService.verifySecret(secret);
		}
		supportMailService.receive(supportMailParser.parse(rawPayload));
		return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
