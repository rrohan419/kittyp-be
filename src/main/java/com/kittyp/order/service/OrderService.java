/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import com.kittyp.cart.dto.CartCheckoutRequest;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.order.dto.OrderFilterDto;
import com.kittyp.order.model.OrderModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderService {

	PaginationModel<OrderModel> allOrderByFilter(OrderFilterDto orderFilterDto, Integer pageNumber,
			Integer pageSize);
		
	OrderModel orderDetailsByOrderNumber(String orderNumber);
	
	// OrderModel updateOrderStatus(OrderStatusUpdateDto orderQuantityUpdateDto);
	
	// OrderModel latestCreatedCartByUser(String userUuid);
	
	/**
     * Creates an order from the cart with shipping and billing details
     * @param userUuid User's UUID
     * @param request Checkout details including addresses
     * @return Created order details
     */
    OrderModel createOrderFromCart(String userUuid, CartCheckoutRequest request);
}
