/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.order.entity.OrderItem;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{

}
