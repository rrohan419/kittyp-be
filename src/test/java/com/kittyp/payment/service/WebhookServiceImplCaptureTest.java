package com.kittyp.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.util.Mapper;
import com.kittyp.payment.entity.WebhookEvent;
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.model.RazorpayResponseModel.Payload;
import com.kittyp.payment.model.RazorpayResponseModel.PaymentEntity;
import com.kittyp.payment.model.RazorpayResponseModel.PaymentWrapper;
import com.kittyp.payment.repository.WebhookEventRepository;

class WebhookServiceImplCaptureTest {

	private WebhookEventRepository webhookEventRepository;
	private CaptureProbe captureProbe;
	private WebhookServiceImpl service;

	@BeforeEach
	void setUp() {
		webhookEventRepository = mock(WebhookEventRepository.class);
		captureProbe = new CaptureProbe();
		Mapper mapper = new Mapper(new ModelMapper(), new ObjectMapper(), mock(Environment.class));
		service = new WebhookServiceImpl(mapper, webhookEventRepository, captureProbe);
	}

	@Test
	void paymentCaptured_completesOnce() {
		when(webhookEventRepository.findByPaymentIdAndEventType("pay_1", "payment.captured"))
				.thenReturn(java.util.Optional.empty());
		when(webhookEventRepository.save(org.mockito.ArgumentMatchers.any(WebhookEvent.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		service.razorpayWebbhook(capturedEvent("payment.captured"));

		assertEquals("order_1", captureProbe.orderId);
		assertEquals("pay_1", captureProbe.paymentId);
	}

	@Test
	void paymentCaptured_replayIsNoOp() {
		when(webhookEventRepository.findByPaymentIdAndEventType("pay_1", "payment.captured"))
				.thenReturn(java.util.Optional.of(new WebhookEvent()));

		service.razorpayWebbhook(capturedEvent("payment.captured"));

		assertNull(captureProbe.orderId);
	}

	@Test
	void paymentAuthorized_doesNotComplete() {
		when(webhookEventRepository.findByPaymentIdAndEventType("pay_1", "payment.authorized"))
				.thenReturn(java.util.Optional.empty());
		when(webhookEventRepository.save(org.mockito.ArgumentMatchers.any(WebhookEvent.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		service.razorpayWebbhook(capturedEvent("payment.authorized"));

		assertNull(captureProbe.orderId);
	}

	private static RazorpayResponseModel capturedEvent(String event) {
		PaymentEntity entity = new PaymentEntity();
		entity.setId("pay_1");
		entity.setOrder_id("order_1");
		entity.setStatus("captured");
		PaymentWrapper wrapper = new PaymentWrapper();
		wrapper.setEntity(entity);
		Payload payload = new Payload();
		payload.setPayment(wrapper);
		RazorpayResponseModel model = new RazorpayResponseModel();
		model.setEvent(event);
		model.setPayload(payload);
		return model;
	}

	static class CaptureProbe extends PaymentCaptureService {
		String orderId;
		String paymentId;

		CaptureProbe() {
			super(null, null, null, null, null);
		}

		@Override
		public void completeCaptured(String razorpayOrderId, String paymentId) {
			this.orderId = razorpayOrderId;
			this.paymentId = paymentId;
		}
	}
}
