/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;

/**
 * @author rrohan419@gmail.com 
 */
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order>{

	List<Order> findByUser_Uuid(String uuid);
	
	Order findByUser_UuidAndStatus(String uuid, OrderStatus status);
	
	Optional<Order> findByOrderNumber(String orderNumber);
	
	@Query("SELECT COALESCE(MAX(CAST(SUBSTRING(z.orderNumber, 7, LENGTH(z.orderNumber) - 6) AS int)), 0) " +
            "FROM Order z WHERE z.orderNumber LIKE 'IND-KP%'")
    Long findMaxOrderNumber();
	
	Order findByAggregatorOrderNumber(String aggregatorOrderNumber);

	Integer countByUser_EmailAndStatusIn(String email, List<OrderStatus> status);

	Integer countByIsActiveAndStatusIn(boolean isActive, List<OrderStatus> status);

	@Query(value = """
			SELECT CAST(created_at AS date), COUNT(*)
			FROM orders
			WHERE is_active = true
			  AND status IN (:statuses)
			  AND created_at >= :from AND created_at < :to
			GROUP BY 1
			ORDER BY 1
			""", nativeQuery = true)
	List<Object[]> countCreatedByDay(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("statuses") List<String> statuses);
}
