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
import com.kittyp.payment.model.RazorpayResponseModel;
import com.kittyp.payment.service.WebhookService;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class WebhookController {

	private final ApiResponse<?> responseBuilder;
	private final WebhookService webhookService;
	
	@PostMapping("/webhook/razorpay")
  public ResponseEntity<SuccessResponse<String>> createOrder(@RequestBody RazorpayResponseModel razorpayResponseModel) {
      		
		webhookService.razorpayWebbhook(razorpayResponseModel);
      return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
  }
}
