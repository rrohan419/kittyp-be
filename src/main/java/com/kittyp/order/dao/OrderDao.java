/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderDao {
	
	Order saveOrder(Order order);
	
	Order orderByOrderNumber(String orderNumber);
	
	
	Order orderByAggregatorOrderNumber(String aggregatorOrderNumber);
	Order getLastCreatedOrder(String userUuid);
	
	Page<Order> findAllOrders(Pageable pageable, Specification<Order> specification);

	Integer countOfSuccessfullOrderByUser(String email);

	Integer countOfOrderByStatus(boolean isActive, List<OrderStatus> status);;
}
