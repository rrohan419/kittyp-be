/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.kittyp.common.model.ApiResponse;
import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.dto.RazorpayVerificationRequest;
import com.kittyp.payment.model.CreateOrderModel;
import com.kittyp.payment.service.RazorPayService;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@RestController
@RequestMapping("/api/v1/razorpay")
@RequiredArgsConstructor
public class RazorPayController {

	private final RazorPayService razorPayService;

	@PostMapping("/create-order")
	public ResponseEntity<ApiResponse<CreateOrderModel>> createOrder(@RequestBody RazorPayOrderRequestDto orderRequestDto) {
		CreateOrderModel orderModel = razorPayService.createOrder(orderRequestDto);
		return ResponseEntity.ok(new ApiResponse<>(true, "Order created successfully", orderModel));
	}

	@PostMapping("/verify-payment")
	public ResponseEntity<ApiResponse<String>> verifyPayment(@RequestBody RazorpayVerificationRequest verificationRequest) throws RazorpayException {
		String response = razorPayService.verifyPayment(verificationRequest);
		return ResponseEntity.ok(new ApiResponse<>(true, "Payment verification successful", response));
	}

	@PostMapping("/handle-timeout/{orderId}")
	public ResponseEntity<ApiResponse<String>> handlePaymentTimeout(@PathVariable String orderId) {
		razorPayService.handlePaymentTimeout(orderId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Payment timeout handled successfully", "Order marked as timed out"));
	}

	@PostMapping("/handle-cancellation/{orderId}")
	public ResponseEntity<ApiResponse<String>> handlePaymentCancellation(@PathVariable String orderId) {
		razorPayService.handlePaymentCancellation(orderId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Payment cancellation handled successfully", "Order marked as cancelled"));
	}
}
