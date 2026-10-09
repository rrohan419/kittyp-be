package com.kittyp.contact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.util.ClientIp;
import com.kittyp.contact.dto.ContactRequest;
import com.kittyp.contact.service.ContactRateLimiter;
import com.kittyp.contact.service.ContactService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PublicContactController {

	private final ContactService contactService;
	private final ContactRateLimiter contactRateLimiter;
	private final ApiResponse<?> responseBuilder;

	@PostMapping(ApiUrl.PUBLIC_CONTACT)
	public ResponseEntity<SuccessResponse<Void>> submit(@Valid @RequestBody ContactRequest request,
			HttpServletRequest http) {
		contactRateLimiter.check(ClientIp.from(http));
		contactService.send(request);
		return responseBuilder.buildSuccessResponse(null, "Message sent", HttpStatus.OK);
	}
}
