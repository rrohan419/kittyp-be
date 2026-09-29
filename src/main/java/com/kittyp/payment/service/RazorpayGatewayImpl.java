package com.kittyp.payment.service;

import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.payment.constants.RazorPayConstant;
import com.kittyp.payment.model.CreateOrderModel;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RazorpayGatewayImpl implements RazorpayGateway {

	private final Environment env;
	private final Mapper mapper;

	@Override
	public CreateOrderModel createOrder(JSONObject request) {
		try {
			Order order = client().orders.create(request);
			return mapper.convertJsonToObejct(order.toJson(), CreateOrderModel.class);
		} catch (RazorpayException e) {
			throw new CustomException("Failed to create Razorpay order", HttpStatus.BAD_REQUEST, e);
		}
	}

	@Override
	public CreateOrderModel fetchOrder(String razorpayOrderId) {
		try {
			Order order = client().orders.fetch(razorpayOrderId);
			return mapper.convertJsonToObejct(order.toJson(), CreateOrderModel.class);
		} catch (RazorpayException e) {
			throw new CustomException("Failed to fetch Razorpay order", HttpStatus.BAD_REQUEST, e);
		}
	}

	private RazorpayClient client() {
		try {
			return new RazorpayClient(env.getProperty(RazorPayConstant.KEY_ID),
					env.getProperty(RazorPayConstant.KEY_SECRET));
		} catch (Exception e) {
			throw new CustomException("Error initializing razorpay client", HttpStatus.SERVICE_UNAVAILABLE, e);
		}
	}
}
