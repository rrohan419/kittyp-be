package com.kittyp.payment.service;

import org.json.JSONObject;

import com.kittyp.payment.model.CreateOrderModel;

public interface RazorpayGateway {

	CreateOrderModel createOrder(JSONObject request);

	CreateOrderModel fetchOrder(String razorpayOrderId);

	/**
	 * Fetches the payment record from Razorpay so the server can reconcile the
	 * captured amount/currency/order independently of the client. Returns a
	 * JSONObject containing at least: amount (paise), currency, order_id, status.
	 */
	JSONObject fetchPayment(String paymentId);
}
