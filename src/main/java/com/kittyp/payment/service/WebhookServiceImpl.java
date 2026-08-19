/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kittyp.common.util.Mapper;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.payment.entity.WebhookEvent;
import com.kittyp.payment.enums.WebhookSource;
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.model.RazorpayResponseModel.PaymentEntity;
import com.kittyp.payment.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

	private static final Logger logger = LoggerFactory.getLogger(WebhookServiceImpl.class);
	static final String PROCESSED = "PROCESSED";
	static final String RECEIVED = "RECEIVED";
	static final String FAILED = "FAILED";

	private final Mapper mapper;
	private final WebhookEventRepository webhookEventRepository;
	private final PaymentCaptureService paymentCaptureService;

	@Override
	@Transactional
	public void razorpayWebbhook(RazorpayResponseModel razorpayResponseModel) {
		if (razorpayResponseModel == null || razorpayResponseModel.getPayload() == null
				|| razorpayResponseModel.getPayload().getPayment() == null
				|| razorpayResponseModel.getPayload().getPayment().getEntity() == null) {
			logger.warn("Razorpay webhook missing payment entity");
			return;
		}

		PaymentEntity paymentEntity = razorpayResponseModel.getPayload().getPayment().getEntity();
		String eventType = razorpayResponseModel.getEvent();
		String paymentId = paymentEntity.getId();
		String orderId = paymentEntity.getOrder_id();

		if (StringUtils.hasText(paymentId) && StringUtils.hasText(eventType)
				&& webhookEventRepository.findByPaymentIdAndEventType(paymentId, eventType).isPresent()) {
			logger.info("Skipping duplicate Razorpay webhook {} for payment {}", eventType, paymentId);
			return;
		}

		WebhookEvent webhookEvent = new WebhookEvent();
		webhookEvent.setSource(WebhookSource.RAZORPAY);
		webhookEvent.setEventType(eventType);
		webhookEvent.setPayload(mapper.convertObjectToJson(razorpayResponseModel));
		webhookEvent.setPaymentId(paymentId);
		webhookEvent.setStatus(RECEIVED);
		webhookEvent.setErrorMessage(paymentEntity.getError_reason());
		webhookEvent.setRetryCount(0);
		webhookEvent.setOrderId(orderId);
		webhookEventRepository.save(webhookEvent);

		OrderStatus mapped = eventType != null ? OrderStatus.fromRazorpayStatus(eventType) : OrderStatus.UNKNOWN;
		try {
			if (mapped == OrderStatus.SUCCESSFULL) {
				paymentCaptureService.completeCaptured(orderId, paymentId);
				webhookEvent.setStatus(PROCESSED);
			} else if (mapped == OrderStatus.FAILED) {
				paymentCaptureService.failStoreIfPending(orderId);
				webhookEvent.setStatus(PROCESSED);
			} else {
				logger.info("Ignoring Razorpay event {} for order {}", eventType, orderId);
			}
			webhookEventRepository.save(webhookEvent);
		} catch (Exception e) {
			webhookEvent.setStatus(FAILED);
			webhookEvent.setErrorMessage(e.getMessage());
			webhookEventRepository.save(webhookEvent);
			logger.error("Error processing Razorpay webhook for order {}: {}", orderId, e.getMessage(), e);
			throw e;
		}
	}
}
