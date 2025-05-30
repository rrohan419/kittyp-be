/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.json.JSONObject;
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
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rrohan419@gmail.com 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RazorPayServiceImpl implements RazorPayService {

	private final Environment env;
	private final Mapper mapper;
	private final OrderDao orderDao;
	private final InvoiceService invoiceService;
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CreateOrderModel createOrder(RazorPayOrderRequestDto orderRequestDto) {		
		RazorpayClient razorpayClient;
//		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		try {
			razorpayClient = new RazorpayClient(env.getProperty(RazorPayConstant.KEY_ID), env.getProperty(RazorPayConstant.KEY_SECRET));
        } catch (Exception e) {
        	throw new CustomException("Error initializing razorpay client", HttpStatus.SERVICE_UNAVAILABLE , e);
        }
		
		JSONObject orderRequest = new JSONObject();
		orderRequest.put(RazorPayConstant.AMOUNT,  orderRequestDto.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
		orderRequest.put(RazorPayConstant.CURRENCY,orderRequestDto.getCurrency());
		orderRequest.put(RazorPayConstant.RECEIPT, orderRequestDto.getReceipt());
		orderRequest.put(RazorPayConstant.NOTES, orderRequestDto.getNotes() != null ? new JSONObject(orderRequestDto.getNotes()) : new JSONObject());

		try {
			// First make the external API call
			Order order = razorpayClient.orders.create(orderRequest);
			log.info("Razorpay order created successfully with ID: " + order.get("id"));
			CreateOrderModel orderModel = mapper.convertJsonToObejct(order.toJson(), CreateOrderModel.class);
			
			// Then update our database within the transaction
			com.kittyp.order.entity.Order dbOrder = orderDao.orderByOrderNumber(orderRequestDto.getReceipt());
			if (dbOrder == null) {
				throw new CustomException("Order not found", HttpStatus.NOT_FOUND);
			}
			dbOrder.setAggregatorOrderNumber(orderModel.getId());
			OrderStatus status = OrderStatus.fromRazorpayStatus(orderModel.getStatus());
			dbOrder.setStatus(status);
			dbOrder.setTotalAmount(orderRequestDto.getAmount());
			dbOrder.setTaxes(orderRequestDto.getTaxes());
			dbOrder.setCreatedAt(LocalDateTime.now());
			orderDao.saveOrder(dbOrder);
			
			return orderModel;
		} catch (RazorpayException e) {
			throw new CustomException("Failed to create Razorpay order", HttpStatus.BAD_REQUEST , e);
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
				order.setStatus(OrderStatus.SUCCESSFULL);
				order = orderDao.saveOrder(order);
				
				// Generate invoice after successful database update
				invoiceService.generateInvoiceAndSaveInS3(order.getOrderNumber());
				
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

}
