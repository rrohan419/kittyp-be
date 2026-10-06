package com.kittyp.email.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.service.EmailAuditService;

@ExtendWith(MockitoExtension.class)
class ZeptoMailWebhookControllerTest {

	@Mock
	private EmailAuditService emailAuditService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ZeptoMailWebhookController controller = new ZeptoMailWebhookController(new ApiResponse<>(), emailAuditService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new TestExceptionAdvice()).build();
	}

	@Test
	void validCallReturnsOk() throws Exception {
		mockMvc.perform(post("/api/v1/webhook/zeptomail")
				.header(ZeptoMailWebhookController.SECRET_HEADER, "zepto-secret")
				.content("{\"event_name\":[\"delivered\"]}"))
				.andExpect(status().isOk());

		verify(emailAuditService).receiveZeptoWebhook("zepto-secret", "{\"event_name\":[\"delivered\"]}");
	}

	@Test
	void missingSecretReturnsUnauthorized() throws Exception {
		doThrow(new CustomException("Missing webhook secret", HttpStatus.UNAUTHORIZED))
				.when(emailAuditService).receiveZeptoWebhook(null, "{}");

		mockMvc.perform(post("/api/v1/webhook/zeptomail").content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidSecretReturnsUnauthorized() throws Exception {
		doThrow(new CustomException("Invalid webhook secret", HttpStatus.UNAUTHORIZED))
				.when(emailAuditService).receiveZeptoWebhook("wrong", "{}");

		mockMvc.perform(post("/api/v1/webhook/zeptomail")
				.header(ZeptoMailWebhookController.SECRET_HEADER, "wrong")
				.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void malformedPayloadReturnsBadRequest() throws Exception {
		doThrow(new CustomException("Invalid webhook payload", HttpStatus.BAD_REQUEST))
				.when(emailAuditService).receiveZeptoWebhook("zepto-secret", "{");

		mockMvc.perform(post("/api/v1/webhook/zeptomail")
				.header(ZeptoMailWebhookController.SECRET_HEADER, "zepto-secret")
				.content("{"))
				.andExpect(status().isBadRequest());
	}

	@RestControllerAdvice
	static class TestExceptionAdvice {
		@ExceptionHandler(CustomException.class)
		ResponseEntity<String> handle(CustomException ex) {
			return ResponseEntity.status(ex.getHttpStatus()).body(ex.getMessage());
		}
	}
}
