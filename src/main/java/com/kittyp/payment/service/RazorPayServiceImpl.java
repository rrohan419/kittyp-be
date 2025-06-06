/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.payment.constants.RazorPayConstant;
import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.dto.RazorpayVerificationRequest;
import com.kittyp.payment.model.CreateOrderModel;
import com.kittyp.product.service.ProductService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class RazorPayServiceImpl implements RazorPayService {

	private static final Logger logger = LoggerFactory.getLogger(RazorPayServiceImpl.class);
	private final Environment env;
	private final Mapper mapper;
	private final OrderDao orderDao;
	private final InvoiceService invoiceService;
	private final ProductService productService;
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CreateOrderModel createOrder(RazorPayOrderRequestDto orderRequestDto) {		
		RazorpayClient razorpayClient;
		try {
			razorpayClient = new RazorpayClient(env.getProperty(RazorPayConstant.KEY_ID), env.getProperty(RazorPayConstant.KEY_SECRET));
		} catch (Exception e) {
			throw new CustomException("Error initializing razorpay client", HttpStatus.SERVICE_UNAVAILABLE , e);
		}
		
		// First get our order and validate/reserve stock
		com.kittyp.order.entity.Order dbOrder = orderDao.orderByOrderNumber(orderRequestDto.getReceipt());
		if (dbOrder == null) {
			throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
		}

		try {
			for (var orderItem : dbOrder.getOrderItems()) {
				productService.validateProductStock(orderItem.getProduct().getUuid(), orderItem.getQuantity());
				productService.reserveStock(orderItem.getProduct().getUuid(), orderItem.getQuantity(), dbOrder.getOrderNumber());
			}
			logger.info("Successfully reserved stock for order: {}", dbOrder.getOrderNumber());
		} catch (Exception e) {
			// If stock reservation fails, we need to cancel any reservations we've made
			try {
				productService.cancelStockReservation(dbOrder.getOrderNumber());
			} catch (Exception ce) {
				logger.error("Failed to cleanup stock reservations after failure for order: {}", dbOrder.getOrderNumber(), ce);
			}
			throw new CustomException("Failed to reserve stock: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		
		JSONObject orderRequest = new JSONObject();
		orderRequest.put(RazorPayConstant.AMOUNT, orderRequestDto.getAmount().multiply(BigDecimal.valueOf(100L)).intValue());
		orderRequest.put(RazorPayConstant.CURRENCY, orderRequestDto.getCurrency());
		orderRequest.put(RazorPayConstant.RECEIPT, orderRequestDto.getReceipt());
		orderRequest.put(RazorPayConstant.NOTES, orderRequestDto.getNotes() != null ? new JSONObject(orderRequestDto.getNotes()) : new JSONObject());

		try {
			// Create Razorpay order
			Order order = razorpayClient.orders.create(orderRequest);
			logger.info("Razorpay order created successfully with ID: " + order.get("id"));
			CreateOrderModel orderModel = mapper.convertJsonToObejct(order.toJson(), CreateOrderModel.class);
			
			// Update our order with Razorpay details
			dbOrder.setAggregatorOrderNumber(orderModel.getId());
			dbOrder.setStatus(OrderStatus.PAYMENT_PENDING);
			dbOrder.setTotalAmount(orderRequestDto.getAmount());
			dbOrder.setTaxes(orderRequestDto.getTaxes());
			dbOrder.setCreatedAt(LocalDateTime.now());
			orderDao.saveOrder(dbOrder);
			
			return orderModel;
		} catch (RazorpayException e) {
			// If Razorpay order creation fails, cancel the stock reservations
			try {
				productService.cancelStockReservation(dbOrder.getOrderNumber());
				logger.info("Cancelled stock reservations after Razorpay order creation failure for order: {}", dbOrder.getOrderNumber());
			} catch (Exception ce) {
				logger.error("Failed to cleanup stock reservations after Razorpay failure for order: {}", dbOrder.getOrderNumber(), ce);
			}
			throw new CustomException("Failed to create Razorpay order", HttpStatus.BAD_REQUEST, e);
		}
	}
	/**
	 * @author rrohan419@gmail.com
	 * @throws RazorpayException 
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public String verifyPayment(RazorpayVerificationRequest verificationRequest) throws RazorpayException {
		if (verificationRequest.getOrderId() == null || verificationRequest.getPaymentId() == null || verificationRequest.getSignature() == null) {
			throw new CustomException("Missing required payment verification parameters", HttpStatus.BAD_REQUEST);
		}

		JSONObject options = new JSONObject();
		options.put("razorpay_order_id", verificationRequest.getOrderId());
		options.put("razorpay_payment_id", verificationRequest.getPaymentId());
		options.put("razorpay_signature", verificationRequest.getSignature());
		
		try {
			boolean isValid = Utils.verifyPaymentSignature(options, env.getProperty(RazorPayConstant.KEY_SECRET));
			
			if (isValid) {
				// Update order status within transaction
				com.kittyp.order.entity.Order order = orderDao.orderByAggregatorOrderNumber(verificationRequest.getOrderId());
				if (order == null) {
					throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
				}

				// First confirm the stock reservation
				try {
					productService.confirmStockReservation(order.getOrderNumber());
					logger.info("Successfully confirmed stock reservation for order: {}", order.getOrderNumber());
				} catch (Exception e) {
					logger.error("Failed to confirm stock reservation for order: {}", order.getOrderNumber(), e);
					// We don't throw here as payment is already verified
					// This needs manual intervention to resolve stock discrepancy
				}

				// Then update order status and generate invoice
				order.setStatus(OrderStatus.SUCCESSFULL);
				order = orderDao.saveOrder(order);
				
				// Generate invoice after successful database update
				invoiceService.generateInvoiceAndSaveInS3(order.getOrderNumber(), order.getUser().getUuid());
				
				return "Payment verified successfully";
			} else {
				throw new CustomException("Invalid payment signature", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			if (e instanceof CustomException) {
				throw e;
			}
			throw new CustomException("Payment verification failed", HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public void handlePaymentTimeout(String orderId) {
		try {
			com.kittyp.order.entity.Order order = orderDao.orderByAggregatorOrderNumber(orderId);
			if (order != null && order.getStatus() == OrderStatus.PAYMENT_PENDING) {
				// First cancel the stock reservation
				try {
					productService.cancelStockReservation(order.getOrderNumber());
					logger.info("Successfully cancelled stock reservation for timed out order: {}", order.getOrderNumber());
				} catch (Exception e) {
					logger.error("Failed to cancel stock reservation for timed out order: {}", order.getOrderNumber(), e);
					// Continue with order status update even if reservation cancellation fails
					// This needs manual intervention to resolve stock discrepancy
				}

				// Then update order status
				order.setStatus(OrderStatus.PAYMENT_TIMEOUT);
				orderDao.saveOrder(order);
				logger.info("Order {} marked as timed out", orderId);
			}
		} catch (Exception e) {
			logger.error("Error handling payment timeout for order {}: {}", orderId, e.getMessage());
			throw new CustomException("Failed to handle payment timeout", HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public void handlePaymentCancellation(String orderId) {
		try {
			com.kittyp.order.entity.Order order = orderDao.orderByAggregatorOrderNumber(orderId);
			if (order != null && order.getStatus() == OrderStatus.PAYMENT_PENDING) {
				// First cancel the stock reservation
				try {
					productService.cancelStockReservation(order.getOrderNumber());
					logger.info("Successfully cancelled stock reservation for cancelled order: {}", order.getOrderNumber());
				} catch (Exception e) {
					logger.error("Failed to cancel stock reservation for cancelled order: {}", order.getOrderNumber(), e);
					// Continue with order status update even if reservation cancellation fails
					// This needs manual intervention to resolve stock discrepancy
				}

				// Then update order status
				order.setStatus(OrderStatus.PAYMENT_CANCELLED);
				orderDao.saveOrder(order);
				logger.info("Order {} marked as cancelled", orderId);
			}
		} catch (Exception e) {
			logger.error("Error handling payment cancellation for order {}: {}", orderId, e.getMessage());
			throw new CustomException("Failed to handle payment cancellation", HttpStatus.INTERNAL_SERVER_ERROR, e);
		}
	}

}
