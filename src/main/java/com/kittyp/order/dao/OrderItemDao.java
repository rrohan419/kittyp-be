/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dao;

import java.util.List;

import com.kittyp.order.entity.OrderItem;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderItemDao {

	List<OrderItem> saveAllOrderItems(List<OrderItem> orderItems);
	
	void deleteAllOrderItems(List<OrderItem> orderItems);
}
