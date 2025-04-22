/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dao;

import java.util.List;

import com.kittyp.order.entity.Order;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderDao {

	List<Order> ordersByUserUuid(String uuid);
	
	Order saveOrder(Order order);
	
	Order orderByOrderNumber(String orderNumber);
	
	Order getLastCreatedOrder(String userUuid);
}
