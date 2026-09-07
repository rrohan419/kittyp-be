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
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import org.springframework.http.HttpStatus;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.payment.entity.WebhookEvent;
import com.kittyp.payment.enums.WebhookSource;
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.model.RazorpayResponseModel.PaymentEntity;
import com.kittyp.payment.repository.WebhookEventRepository;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
	private final OrderDao orderDao;
	private final ConsultationInvoiceRepository consultationInvoiceRepository;
	private final TreatmentInvoiceService treatmentInvoiceService;

@Override
    @Transactional
    public void razorpayWebhook(RazorpayResponseModel razorpayResponseModel) {
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
				requireCapturedAmountMatches(orderId, paymentEntity);
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

	/**
	 * Reconciler for the server-captured payment amount. The webhook payload is
	 * Razorpay-signed, but we still verify the captured amount matches what the
	 * order/invoice expects before marking anything paid — defense-in-depth
	 * against mis-priced or tampered orders.
	 */
	private void requireCapturedAmountMatches(String orderId, PaymentEntity paymentEntity) {
		if (paymentEntity.getAmount() < 0) {
			throw new CustomException("Payment amount missing", HttpStatus.BAD_REQUEST);
		}
		int paidPaise = paymentEntity.getAmount();
		String paidCurrency = paymentEntity.getCurrency();

		com.kittyp.order.entity.Order order = orderDao.orderByAggregatorOrderNumber(orderId);
		int expectedPaise;
		if (order != null) {
			BigDecimal amount = order.getTotalAmount();
			if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new CustomException("Order amount is invalid", HttpStatus.BAD_REQUEST);
			}
			if (paidCurrency != null && !paidCurrency.isBlank()
					&& order.getCurrency() != null && !paidCurrency.equalsIgnoreCase(order.getCurrency().name())) {
				throw new CustomException("Payment currency mismatch", HttpStatus.BAD_REQUEST);
			}
			expectedPaise = toPaise(amount);
		} else {
			ConsultationInvoice invoice = consultationInvoiceRepository.findByRazorpayOrderId(orderId)
					.orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
			BigDecimal amount = treatmentInvoiceService.remainingBalance(invoice);
			if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new CustomException("Invoice amount is invalid", HttpStatus.BAD_REQUEST);
			}
			if (paidCurrency != null && !paidCurrency.isBlank() && invoice.getCurrency() != null
					&& !paidCurrency.equalsIgnoreCase(invoice.getCurrency())) {
				throw new CustomException("Payment currency mismatch", HttpStatus.BAD_REQUEST);
			}
			expectedPaise = toPaise(amount);
		}

		if (paidPaise != expectedPaise) {
			throw new CustomException("Payment amount does not match order", HttpStatus.BAD_REQUEST);
		}
	}

	private static int toPaise(BigDecimal amount) {
		return amount.multiply(BigDecimal.valueOf(100L)).setScale(0, RoundingMode.HALF_UP).intValueExact();
	}
}
