/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import com.kittyp.common.model.PaginationModel;
import com.kittyp.order.dto.OrderDto;
import com.kittyp.order.dto.OrderFilterDto;
import com.kittyp.order.dto.OrderStatusUpdateDto;
import com.kittyp.order.model.OrderModel;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderService {

	PaginationModel<OrderModel> allOrderByFilter(OrderFilterDto orderFilterDto, Integer pageNumber,
			Integer pageSize);
	
	OrderModel createUpdateOrder(OrderDto orderDto);
	
	OrderModel orderDetailsByOrderNumber(String orderNumber);
	
	OrderModel updateOrderStatus(OrderStatusUpdateDto orderQuantityUpdateDto);
	
	OrderModel latestCreatedCartByUser(String userUuid);
}
