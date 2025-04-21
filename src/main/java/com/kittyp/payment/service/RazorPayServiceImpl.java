/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.payment.constants.RazorPayConstant;
import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.model.CreateOrderModel;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class RazorPayServiceImpl implements RazorPayService {

	private final Environment env;
	private final Mapper mapper;
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
		orderRequest.put(RazorPayConstant.AMOUNT,orderRequestDto.getAmount()*100);
		orderRequest.put(RazorPayConstant.CURRENCY,orderRequestDto.getCurrency());
		orderRequest.put(RazorPayConstant.RECEIPT, "receipt#1");
		orderRequest.put(RazorPayConstant.NOTES, new JSONObject(orderRequestDto.getNotes()));

		try {
			Order order = razorpayClient.orders.create(orderRequest);
			
			CreateOrderModel orderModel = mapper.convertJsonToObejct(order.toJson(), CreateOrderModel.class);
			return orderModel;
		} catch (RazorpayException e) {
			throw new CustomException("Not able to create order. Please try again!", HttpStatus.BAD_REQUEST , e);
		}

	}

}
