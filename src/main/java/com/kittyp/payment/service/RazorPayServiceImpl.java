/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.payment.constants.RazorPayConstant;
import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.dto.RazorpayVerificationRequest;
import com.kittyp.payment.enums.RazorpayOrderSource;
import com.kittyp.payment.model.CreateOrderModel;
import com.kittyp.product.service.ProductService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
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
	private final OrderDao orderDao;
	private final ProductService productService;
	private final RazorpayGateway razorpayGateway;
	private final PaymentCaptureService paymentCaptureService;
	private final ConsultationInvoiceRepository consultationInvoiceRepository;
	private final TreatmentInvoiceService treatmentInvoiceService;
	private final ClinicService clinicService;
	private final UserDao userDao;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CreateOrderModel createOrder(RazorPayOrderRequestDto orderRequestDto) {
		if (resolveSource(orderRequestDto) == RazorpayOrderSource.TREATMENT_INVOICE) {
			return createInvoiceOrder(orderRequestDto);
		}
		return createStoreOrder(orderRequestDto);
	}

	private RazorpayOrderSource resolveSource(RazorPayOrderRequestDto request) {
		if (request.getSource() == RazorpayOrderSource.TREATMENT_INVOICE
				|| StringUtils.hasText(request.getInvoiceUuid())) {
			return RazorpayOrderSource.TREATMENT_INVOICE;
		}
		return RazorpayOrderSource.STORE_ORDER;
	}

	private CreateOrderModel createStoreOrder(RazorPayOrderRequestDto orderRequestDto) {
		if (!StringUtils.hasText(orderRequestDto.getReceipt())) {
			throw new CustomException("Order receipt is required", HttpStatus.BAD_REQUEST);
		}

		com.kittyp.order.entity.Order dbOrder = orderDao.orderByOrderNumber(orderRequestDto.getReceipt());
		if (dbOrder == null) {
			throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
		}

		User caller = currentUser();
		if (dbOrder.getUser() == null || dbOrder.getUser().getEmail() == null
				|| !dbOrder.getUser().getEmail().equalsIgnoreCase(caller.getEmail())) {
			throw new CustomException("Not authorized to pay for this order", HttpStatus.FORBIDDEN);
		}

		if (dbOrder.getStatus() == OrderStatus.SUCCESSFULL || dbOrder.getStatus() == OrderStatus.DELIVERED) {
			throw new CustomException("Order is already paid", HttpStatus.CONFLICT);
		}

		if (StringUtils.hasText(dbOrder.getAggregatorOrderNumber())
				&& dbOrder.getStatus() == OrderStatus.PAYMENT_PENDING) {
			return razorpayGateway.fetchOrder(dbOrder.getAggregatorOrderNumber());
		}

		try {
			for (var orderItem : dbOrder.getOrderItems()) {
				productService.validateProductStock(orderItem.getProduct().getUuid(), orderItem.getQuantity());
				productService.reserveStock(orderItem.getProduct().getUuid(), orderItem.getQuantity(),
						dbOrder.getOrderNumber());
			}
			logger.info("Successfully reserved stock for order: {}", dbOrder.getOrderNumber());
		} catch (CustomException e) {
			cleanupStoreReservation(dbOrder.getOrderNumber());
			throw e;
		} catch (Exception e) {
			cleanupStoreReservation(dbOrder.getOrderNumber());
			throw new CustomException("Failed to reserve stock: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}

		BigDecimal amount = dbOrder.getTotalAmount();
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			cleanupStoreReservation(dbOrder.getOrderNumber());
			throw new CustomException("Order amount is invalid", HttpStatus.BAD_REQUEST);
		}

		try {
			JSONObject notes = new JSONObject();
			notes.put("type", RazorpayOrderSource.STORE_ORDER.name());
			notes.put("order_number", dbOrder.getOrderNumber());

			CreateOrderModel orderModel = razorpayGateway.createOrder(
					razorpayOrderRequest(amount, currencyOf(dbOrder), dbOrder.getOrderNumber(), notes));

			dbOrder.setAggregatorOrderNumber(orderModel.getId());
			dbOrder.setStatus(OrderStatus.PAYMENT_PENDING);
			orderDao.saveOrder(dbOrder);
			return orderModel;
		} catch (CustomException e) {
			cleanupStoreReservation(dbOrder.getOrderNumber());
			throw e;
		}
	}

	private CreateOrderModel createInvoiceOrder(RazorPayOrderRequestDto orderRequestDto) {
		if (!StringUtils.hasText(orderRequestDto.getInvoiceUuid())) {
			throw new CustomException("Invoice uuid is required", HttpStatus.BAD_REQUEST);
		}

		ConsultationInvoice invoice = consultationInvoiceRepository.findByUuid(orderRequestDto.getInvoiceUuid())
				.orElseThrow(() -> new ResourceNotFoundException("Consultation invoice", "uuid",
						orderRequestDto.getInvoiceUuid()));

		requireInvoicePayAccess(invoice, currentUser());

		if (treatmentInvoiceService.isRazorpayPaid(invoice)) {
			throw new CustomException("Invoice is already paid", HttpStatus.CONFLICT);
		}

		if (StringUtils.hasText(invoice.getRazorpayOrderId())) {
			return razorpayGateway.fetchOrder(invoice.getRazorpayOrderId());
		}

		BigDecimal amount = treatmentInvoiceService.remainingBalance(invoice);
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new CustomException("Invoice is already paid", HttpStatus.CONFLICT);
		}

		JSONObject notes = new JSONObject();
		notes.put("type", RazorpayOrderSource.TREATMENT_INVOICE.name());
		notes.put("invoice_uuid", invoice.getUuid());

		String receipt = firstNonBlank(invoice.getInvoiceNumber(), invoice.getUuid());
		if (receipt.length() > 40) {
			receipt = receipt.substring(0, 40);
		}

		CreateOrderModel orderModel = razorpayGateway.createOrder(
				razorpayOrderRequest(amount, invoice.getCurrency(), receipt, notes));
		invoice.setRazorpayOrderId(orderModel.getId());
		consultationInvoiceRepository.save(invoice);
		return orderModel;
	}

	private JSONObject razorpayOrderRequest(BigDecimal amount, String currency, String receipt, JSONObject notes) {
		JSONObject orderRequest = new JSONObject();
		orderRequest.put(RazorPayConstant.AMOUNT, toPaise(amount));
		orderRequest.put(RazorPayConstant.CURRENCY, currency != null && !currency.isBlank() ? currency : "INR");
		orderRequest.put(RazorPayConstant.RECEIPT, receipt);
		orderRequest.put(RazorPayConstant.NOTES, notes);
		return orderRequest;
	}

	private int toPaise(BigDecimal amount) {
		return amount.multiply(BigDecimal.valueOf(100L)).setScale(0, RoundingMode.HALF_UP).intValueExact();
	}

	private String currencyOf(com.kittyp.order.entity.Order order) {
		return order.getCurrency() != null ? order.getCurrency().name() : "INR";
	}

	private void requireInvoicePayAccess(ConsultationInvoice invoice, User caller) {
		if (invoice.getDoctor() != null && invoice.getDoctor().getId() != null
				&& invoice.getDoctor().getId().equals(caller.getId())) {
			return;
		}
		if (invoice.getClinic() != null && StringUtils.hasText(invoice.getClinic().getUuid())) {
			clinicService.get(invoice.getClinic().getUuid(), caller.getEmail());
			return;
		}
		throw new CustomException("Not authorized to collect payment for this invoice", HttpStatus.FORBIDDEN);
	}

	private User currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !StringUtils.hasText(authentication.getName())) {
			throw new CustomException("Unauthenticated", HttpStatus.UNAUTHORIZED);
		}
		User user = userDao.userByEmail(authentication.getName());
		if (user == null) {
			throw new CustomException("User not found", HttpStatus.UNAUTHORIZED);
		}
		return user;
	}

	private void cleanupStoreReservation(String orderNumber) {
		try {
			productService.cancelStockReservation(orderNumber);
		} catch (Exception ce) {
			logger.error("Failed to cleanup stock reservations for order: {}", orderNumber, ce);
		}
	}

	private static String firstNonBlank(String first, String second) {
		if (StringUtils.hasText(first)) {
			return first.trim();
		}
		return second;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public String verifyPayment(RazorpayVerificationRequest verificationRequest) throws RazorpayException {
		if (verificationRequest.getOrderId() == null || verificationRequest.getPaymentId() == null
				|| verificationRequest.getSignature() == null) {
			throw new CustomException("Missing required payment verification parameters", HttpStatus.BAD_REQUEST);
		}

		JSONObject options = new JSONObject();
		options.put("razorpay_order_id", verificationRequest.getOrderId());
		options.put("razorpay_payment_id", verificationRequest.getPaymentId());
		options.put("razorpay_signature", verificationRequest.getSignature());

		try {
			boolean isValid = Utils.verifyPaymentSignature(options, env.getProperty(RazorPayConstant.KEY_SECRET));
			if (!isValid) {
				throw new CustomException("Invalid payment signature", HttpStatus.BAD_REQUEST);
			}
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException("Payment verification failed", HttpStatus.INTERNAL_SERVER_ERROR, e);
		}

		paymentCaptureService.completeCaptured(verificationRequest.getOrderId(), verificationRequest.getPaymentId());
		return "Payment verified successfully";
	}

	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public void handlePaymentTimeout(String orderId) {
		try {
			com.kittyp.order.entity.Order order = orderDao.orderByAggregatorOrderNumber(orderId);
			if (order != null && order.getStatus() == OrderStatus.PAYMENT_PENDING) {
				try {
					productService.cancelStockReservation(order.getOrderNumber());
					logger.info("Successfully cancelled stock reservation for timed out order: {}",
							order.getOrderNumber());
				} catch (Exception e) {
					logger.error("Failed to cancel stock reservation for timed out order: {}", order.getOrderNumber(),
							e);
				}

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
				try {
					productService.cancelStockReservation(order.getOrderNumber());
					logger.info("Successfully cancelled stock reservation for cancelled order: {}",
							order.getOrderNumber());
				} catch (Exception e) {
					logger.error("Failed to cancel stock reservation for cancelled order: {}", order.getOrderNumber(),
							e);
				}

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
