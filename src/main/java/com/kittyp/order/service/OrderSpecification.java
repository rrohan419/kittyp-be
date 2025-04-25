/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.common.constants.KeyConstant;
import com.kittyp.order.dto.OrderFilterDto;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.user.entity.User;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * @author rrohan419@gmail.com 
 */
public class OrderSpecification {

	private OrderSpecification() {}
	
	public static Specification<Order> articlesByFilters(OrderFilterDto orderFilterDto) {
		return (Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			
			// Filter for active orders
	        predicates.add(builder.equal(root.get(KeyConstant.IS_ACTIVE), true));
	        predicates.add(builder.notEqual(root.get("status"), OrderStatus.CREATED));
			
			 // Join with the User entity to filter by user UUID
	        if (orderFilterDto.getUserUuid() != null) {
	            Join<Order, User> userJoin = root.join("user");
	            predicates.add(builder.equal(userJoin.get("uuid"), orderFilterDto.getUserUuid()));
	        }

	        // Filter by orderStatus
	        if (orderFilterDto.getOrderStatus() != null) {
	            predicates.add(builder.equal(root.get("status"), orderFilterDto.getOrderStatus()));
	        }
	        
	        // Filter by orderNumber
	        if (orderFilterDto.getOrderNumber() != null && !orderFilterDto.getOrderNumber().isEmpty()) {
//	            predicates.add(builder.equal(root.get("orderNumber"), orderFilterDto.getOrderNumber()));
	            predicates.add(builder.like(builder.lower(root.get("orderNumber")), "%" + orderFilterDto.getOrderNumber().toLowerCase() + "%"));
	        }
	        
	        

	        return builder.and(predicates.toArray(new Predicate[0]));
		};
	
	}
}
