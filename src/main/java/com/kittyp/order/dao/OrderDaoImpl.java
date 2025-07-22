/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Repository
@RequiredArgsConstructor
public class OrderDaoImpl implements OrderDao {

	private final Environment env;
	private final OrderRepository orderRepository;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Order saveOrder(Order order) {
		try {
			return orderRepository.save(order);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Order orderByOrderNumber(String orderNumber) {
		return orderRepository.findByOrderNumber(orderNumber)
				.orElseThrow(() -> new CustomException(
						String.format(env.getProperty(ExceptionConstant.ORDER_NOT_FOUND), orderNumber),
						HttpStatus.NOT_FOUND));
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Order getLastCreatedOrder(String userUuid) {
		try {
			return orderRepository.findByUser_UuidAndStatus(userUuid, OrderStatus.CREATED);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Order orderByAggregatorOrderNumber(String aggregatorOrderNumber) {
		try {
			return orderRepository.findByAggregatorOrderNumber(aggregatorOrderNumber);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Page<Order> findAllOrders(Pageable pageable, Specification<Order> specification) {
		try {
			return orderRepository.findAll(specification, pageable);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Integer countOfSuccessfullOrderByUser(String email) {
		try {
			return orderRepository.countByUser_EmailAndStatusIn(email, List.of(OrderStatus.SUCCESSFULL, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED));
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Integer countOfOrderByStatus(boolean isActive, List<OrderStatus> status) {
		try {
			return orderRepository.countByIsActiveAndStatusIn(isActive, status);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
