package com.kittyp.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.email.service.PaymentEventPublisher;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.product.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCaptureService {

	private static final Logger logger = LoggerFactory.getLogger(PaymentCaptureService.class);

	private final OrderDao orderDao;
	private final InvoiceService invoiceService;
	private final ProductService productService;
	private final PaymentEventPublisher paymentEventPublisher;
	private final TreatmentInvoiceService treatmentInvoiceService;

	@Transactional
	public void completeCaptured(String razorpayOrderId, String paymentId) {
		if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
			throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
		}

		Order order = orderDao.orderByAggregatorOrderNumber(razorpayOrderId);
		if (order != null) {
			completeStoreOrder(order, paymentId);
			return;
		}

		treatmentInvoiceService.completeRazorpayCapture(razorpayOrderId, paymentId);
	}

	private void completeStoreOrder(Order order, String paymentId) {
		if (order.getStatus() == OrderStatus.SUCCESSFULL || order.getStatus() == OrderStatus.DELIVERED) {
			if (paymentId != null && (order.getPaymentId() == null || order.getPaymentId().isBlank())) {
				order.setPaymentId(paymentId);
				orderDao.saveOrder(order);
			}
			logger.info("Store order {} already complete", order.getOrderNumber());
			return;
		}

		try {
			productService.confirmStockReservation(order.getOrderNumber());
			logger.info("Confirmed stock reservation for order: {}", order.getOrderNumber());
		} catch (Exception e) {
			logger.error("Failed to confirm stock reservation for order: {}", order.getOrderNumber(), e);
		}

		order.setStatus(OrderStatus.SUCCESSFULL);
		if (paymentId != null && !paymentId.isBlank()) {
			order.setPaymentId(paymentId);
		}
		order = orderDao.saveOrder(order);

		String userUuid = order.getUser() != null ? order.getUser().getUuid() : null;
		String email = order.getUser() != null ? order.getUser().getEmail() : null;
		invoiceService.generateInvoiceAndSaveInS3(order.getOrderNumber(), userUuid);
		if (email != null) {
			paymentEventPublisher.publishPaymentSuccess(order.getOrderNumber(), email);
		}
		logger.info("Order {} marked successful and invoice generated", order.getOrderNumber());
	}

	@Transactional
	public void failStoreIfPending(String razorpayOrderId) {
		if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
			return;
		}
		Order order = orderDao.orderByAggregatorOrderNumber(razorpayOrderId);
		if (order == null || order.getStatus() != OrderStatus.PAYMENT_PENDING) {
			return;
		}
		try {
			productService.cancelStockReservation(order.getOrderNumber());
		} catch (Exception e) {
			logger.error("Failed to cancel stock reservation for failed order: {}", order.getOrderNumber(), e);
		}
		order.setStatus(OrderStatus.FAILED);
		orderDao.saveOrder(order);
	}
}
