package com.kittyp.notification.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

/**
 * Public WhatsApp Embedded Signup config for the FE (never exposes app secret).
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PublicWhatsAppController {

	private final ApiResponse<?> responseBuilder;

	@Value("${whatsapp.enabled:false}")
	private boolean whatsappEnabled;

	@Value("${whatsapp.meta-app-id:}")
	private String metaAppId;

	@Value("${whatsapp.embedded-signup-config-id:}")
	private String embeddedSignupConfigId;

	@Value("${whatsapp.api-version:v21.0}")
	private String apiVersion;

	@GetMapping(ApiUrl.PUBLIC_WHATSAPP_EMBEDDED_SIGNUP_CONFIG)
	public ResponseEntity<SuccessResponse<Map<String, Object>>> embeddedSignupConfig() {
		Map<String, Object> map = new LinkedHashMap<>();
		boolean configured = whatsappEnabled
				&& StringUtils.hasText(metaAppId)
				&& StringUtils.hasText(embeddedSignupConfigId);
		map.put("enabled", configured);
		map.put("appId", metaAppId == null ? "" : metaAppId.trim());
		map.put("configId", embeddedSignupConfigId == null ? "" : embeddedSignupConfigId.trim());
		map.put("apiVersion", apiVersion == null || apiVersion.isBlank() ? "v21.0" : apiVersion.trim());
		return responseBuilder.buildSuccessResponse(map, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
