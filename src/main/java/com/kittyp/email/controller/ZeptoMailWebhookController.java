package com.kittyp.email.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.email.service.EmailAuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ZeptoMailWebhookController {

	public static final String SECRET_HEADER = "X-Zeptomail-Webhook-Secret";

	private final ApiResponse<?> responseBuilder;
	private final EmailAuditService emailAuditService;

	@PostMapping(ApiUrl.WEBHOOK_ZEPTOMAIL)
	public ResponseEntity<SuccessResponse<String>> zeptoMail(HttpServletRequest request,
			@RequestBody(required = false) String rawPayload) {
		emailAuditService.receiveZeptoWebhook(request.getHeader(SECRET_HEADER), rawPayload);
		return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
