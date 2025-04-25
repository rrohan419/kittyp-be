/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.math.BigDecimal;

import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class RazorPayServiceImpl implements RazorPayService {

	private final Environment env;
	private final Mapper mapper;
	private final OrderDao orderDao;
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
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
			Order order = razorpayClient.orders.create(orderRequest);
			System.out.println("order -----------------------------------"+order);
			CreateOrderModel orderModel = mapper.convertJsonToObejct(order.toJson(), CreateOrderModel.class);
			
			com.kittyp.order.entity.Order dbOrder = orderDao.orderByOrderNumber(orderRequestDto.getReceipt());
			dbOrder.setAggregatorOrderNumber(orderModel.getId());
			OrderStatus status = OrderStatus.fromRazorpayStatus(orderModel.getStatus());
			dbOrder.setStatus(status);
			dbOrder.setTotalAmount(orderRequestDto.getAmount());
			dbOrder.setTaxes(orderRequestDto.getTaxes());
			
			orderDao.saveOrder(dbOrder);
			
			
			return orderModel;
		} catch (RazorpayException e) {
			throw new CustomException("Not able to create order. Please try again!", HttpStatus.BAD_REQUEST , e);
		}

	}
	/**
	 * @author rrohan419@gmail.com
	 * @throws RazorpayException 
	 */
	@Override
	public String verifyPayment(RazorpayVerificationRequest verificationRequest) throws RazorpayException {
		JSONObject options = new JSONObject();
		options.put("razorpay_order_id", verificationRequest.getOrderId());
		options.put("razorpay_payment_id", verificationRequest.getPaymentId());
		options.put("razorpay_signature", verificationRequest.getSignature());
		
        boolean isValid = Utils.verifyPaymentSignature(options, env.getProperty(RazorPayConstant.KEY_SECRET));
        
//		String generatedSignature = Utils.getHash(body.getOrderId() + "|" + body.getPaymentId(), "HmacSHA256", "YOUR_SECRET");

	    if (isValid) {
	        // success - update DB
	    	com.kittyp.order.entity.Order order = orderDao.orderByAggregatorOrderNumber(verificationRequest.getOrderId());
	    	order.setStatus(OrderStatus.SUCCESSFULL);
	    	orderDao.saveOrder(order);
	        return "Payment verified";
	    } else {
//	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
	    	
	    	throw new CustomException("Invalid signature", HttpStatus.BAD_REQUEST);
	    }
	}

}
