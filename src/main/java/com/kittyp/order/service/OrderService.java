/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import java.util.List;
import java.util.Map;

import com.kittyp.order.dto.OrderDto;
import com.kittyp.order.dto.OrderStatusUpdateDto;
import com.kittyp.order.model.OrderModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderService {

	List<OrderModel> ordersByUserUuid(String uuid);
	
	OrderModel createUpdateOrder(OrderDto orderDto);
	
	OrderModel orderDetailsByOrderNumber(String orderNumber);
	
	OrderModel updateOrderStatus(OrderStatusUpdateDto orderQuantityUpdateDto);
}
