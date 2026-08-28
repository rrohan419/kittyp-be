package com.kittyp.payment.service;

import org.json.JSONObject;

import com.kittyp.payment.model.CreateOrderModel;

public interface RazorpayGateway {

	CreateOrderModel createOrder(JSONObject request);

	CreateOrderModel fetchOrder(String razorpayOrderId);
}
