/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.dto.RazorpayVerificationRequest;
import com.kittyp.payment.model.CreateOrderModel;
import com.kittyp.payment.service.RazorPayService;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class RazorPayController {

	private final ApiResponse responseBuilder;
	private final RazorPayService razorPayService;
	
	
	@PostMapping("/razorpay")
//    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<CreateOrderModel>> createOrder(@RequestBody RazorPayOrderRequestDto orderRequestDto) {
        
//		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
		CreateOrderModel response = razorPayService.createOrder(orderRequestDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

	@PostMapping("/razorpay/verify")
	public ResponseEntity<SuccessResponse<String>> verifyPayment(@RequestBody RazorpayVerificationRequest body) throws Exception {
	    
		String response = razorPayService.verifyPayment(body);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}


}
