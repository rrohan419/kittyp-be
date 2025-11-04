/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.util.Mapper;
import com.kittyp.email.service.ZeptoMailService;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.payment.entity.WebhookEvent;
import com.kittyp.payment.enums.WebhookSource;
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.model.RazorpayResponseModel.PaymentEntity;
import com.kittyp.payment.repository.WebhookEventRepository;
import com.kittyp.product.service.ProductService;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

	private static final Logger logger = LoggerFactory.getLogger(WebhookServiceImpl.class);

	private final Mapper mapper;
	private final WebhookEventRepository webhookEventRepository;
	private final OrderDao orderDao;
	private final ProductService productService;
	private final ZeptoMailService zeptoMailService;

	/**
	 * @author rrohan419@gmail.com
	 */
	// @Async+
	@Transactional
	@Override
	public void razorpayWebbhook(RazorpayResponseModel razorpayResponseModel) {
		try {
			PaymentEntity paymentEntity = razorpayResponseModel.getPayload().getPayment().getEntity();
			logger.info("Razorpay response model: {}", razorpayResponseModel);
			logger.info("Payment entity: {}", paymentEntity);
			String orderId = paymentEntity.getOrder_id();

			logger.info("Processing Razorpay webhook for order: {}", orderId);

			// Save webhook event
			WebhookEvent webhookEvent = new WebhookEvent();
			webhookEvent.setSource(WebhookSource.RAZORPAY);
			webhookEvent.setEventType(razorpayResponseModel.getEvent());
			webhookEvent.setPayload(mapper.convertObjectToJson(razorpayResponseModel));
			webhookEvent.setPaymentId(paymentEntity.getId());
			webhookEvent.setStatus(paymentEntity.getStatus());
			webhookEvent.setErrorMessage(paymentEntity.getError_reason());
			webhookEvent.setRetryCount(0);
			webhookEvent.setOrderId(orderId);
			webhookEventRepository.save(webhookEvent);

			// Update order status
			Order order = orderDao.orderByAggregatorOrderNumber(orderId);

			if (order == null) {
				logger.error("Order not found for orderId: {}", orderId);
				throw new RuntimeException("Order not found: " + orderId);
			}

			try {
				Hibernate.initialize(order.getUser());
				OrderStatus status = OrderStatus.fromRazorpayStatus(razorpayResponseModel.getEvent());
				if (status == OrderStatus.UNKNOWN) {
					logger.warn("Unknown status: {} for order: {}", razorpayResponseModel.getEvent(), orderId);
					return;
				}

				// Handle specific payment statuses
				switch (status) {
					case PAYMENT_PENDING:
						logger.info("Payment pending for order: {}", orderId);
						break;
					case PAYMENT_TIMEOUT:
						logger.info("Payment timed out for order: {}", orderId);
						productService.cancelStockReservation(order.getOrderNumber());
						break;
					case PAYMENT_CANCELLED:
						logger.info("Payment cancelled for order: {}", orderId);
						productService.cancelStockReservation(order.getOrderNumber());
						
						break;
					case SUCCESSFULL:
						logger.info("Payment successful for order: {}", orderId);
						if (order.getStatus() != OrderStatus.SUCCESSFULL) {
							order.setStatus(OrderStatus.SUCCESSFULL);
							orderDao.saveOrder(order);
							try {
								productService.confirmStockReservation(order.getOrderNumber());
								logger.info("Successfully confirmed stock reservation for order: {}",
										order.getOrderNumber());
							} catch (Exception e) {
								logger.error("Failed to confirm stock reservation for order: {}",
										order.getOrderNumber(), e);
							}
							logger.info("Successfully updated order status to {} for order: {}", status, orderId);

							// zeptoMailService.sendOrderConfirmationEmail(order.getUser().getEmail(),
							// 		order.getOrderNumber());

						} else {
							logger.info("Order already in successfull status for order: {}", orderId);
						}

						break;
					default:
						logger.warn("Payment status not handled for status: {} for order: {}", status, orderId);
				}

				order.setStatus(status);
				orderDao.saveOrder(order);
				logger.info("Successfully updated order status to {} for order: {}", status, orderId);

			} catch (Exception e) {
				logger.error("Failed to update order status for orderId: {}", orderId, e);
				throw new RuntimeException("Failed to update order status", e);
			}

		} catch (Exception e) {
			logger.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to process webhook", e);
		}
	}

}
