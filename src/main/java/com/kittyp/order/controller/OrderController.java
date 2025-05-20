/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.controller;

import java.net.URL;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.order.dto.OrderDto;
import com.kittyp.order.dto.OrderFilterDto;
import com.kittyp.order.dto.OrderStatusUpdateDto;
import com.kittyp.order.model.OrderModel;
import com.kittyp.order.service.OrderService;
import com.kittyp.payment.service.InvoiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class OrderController {
	private final ApiResponse responseBuilder;
	private final OrderService orderService;
	private final InvoiceService invoiceService;

	@PostMapping(ApiUrl.ORDER_CREATE)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<OrderModel>> createUpdateOrder(@RequestBody @Valid OrderDto orderDto) {

		OrderModel response = orderService.createUpdateOrder(orderDto);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
	
	@PostMapping(ApiUrl.ORDERS_BY_FILTER)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<PaginationModel<OrderModel>>> userOrders(@RequestBody OrderFilterDto orderFilterDto,
			@RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) int page,
			@RequestParam(defaultValue = KeyConstant.PAGE_SIZE) int size) {
        		
		PaginationModel<OrderModel> response = orderService.allOrderByFilter(orderFilterDto, page, size);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@GetMapping(ApiUrl.ORDER_BASE_URL)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<OrderModel>> orderByOrderNumber(@RequestParam String orderNumber) {
        		
		OrderModel response = orderService.orderDetailsByOrderNumber(orderNumber);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@PostMapping(ApiUrl.ORDER_STATUS_UPDATE)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<OrderModel>> orderStatusyUpdate(@RequestBody OrderStatusUpdateDto orderQuantityUpdateDto) {
        		
		OrderModel response = orderService.updateOrderStatus(orderQuantityUpdateDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@GetMapping(ApiUrl.CREATED_ORDER_BY_USER)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<OrderModel>> userLastCreatedOrders(@PathVariable String userUuid) {
        		
		OrderModel response = orderService.latestCreatedCartByUser(userUuid);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
	
	@GetMapping(ApiUrl.ORDER_INVOICE_BY_USER)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<URL>> orderInvoice(@PathVariable String orderNumber, @RequestParam(required = false) String userUuid) {
        		
		URL response = invoiceService.getInvoicePresignedUrl(orderNumber, userUuid);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
