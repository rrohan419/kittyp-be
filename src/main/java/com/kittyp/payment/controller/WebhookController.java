package com.kittyp.payment.controller;

import org.springframework.beans.factory.annotation.Value;
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
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.service.WebhookService;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class WebhookController {

	private final ApiResponse<?> responseBuilder;
	private final WebhookService webhookService;
	private final Mapper mapper;

	@Value("${razorpay.webhook.secret:}")
	private String webhookSecret;

	@PostMapping("/webhook/razorpay")
	public ResponseEntity<SuccessResponse<String>> razorpayWebhook(
			@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
			@RequestBody String rawPayload) {

		if (signature == null || signature.isBlank() || webhookSecret == null || webhookSecret.isBlank()) {
			throw new CustomException("Missing webhook signature", HttpStatus.UNAUTHORIZED);
		}

		try {
			boolean valid = Utils.verifyWebhookSignature(rawPayload, signature, webhookSecret);
			if (!valid) {
				throw new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
			}
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
		}

		webhookService.razorpayWebhook(mapper.readValueFromString(rawPayload, RazorpayResponseModel.class));
		return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
