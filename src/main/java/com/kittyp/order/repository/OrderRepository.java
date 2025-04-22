/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderRepository extends JpaRepository<Order, Long>{

	List<Order> findByUser_Uuid(String uuid);
	
	Order findByUser_UuidAndStatus(String uuid, OrderStatus status);
	
	Optional<Order> findByOrderNumber(String orderNumber);
	
	@Query("SELECT COALESCE(MAX(CAST(SUBSTRING(z.orderNumber, 7, LENGTH(z.orderNumber) - 6) AS int)), 0) " +
            "FROM Order z WHERE z.orderNumber LIKE 'IND-KP%'")
    Long findMaxOrderNumber();
}
