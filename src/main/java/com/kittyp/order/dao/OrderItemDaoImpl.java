/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.order.entity.OrderItem;
import com.kittyp.order.repository.OrderItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Repository
@RequiredArgsConstructor
public class OrderItemDaoImpl implements OrderItemDao {

	private final Environment env;
	private final OrderItemRepository orderItemRepository; 
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public List<OrderItem> saveAllOrderItems(List<OrderItem> orderItems) {
		try {
			return orderItemRepository.saveAll(orderItems);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void deleteAllOrderItems(List<OrderItem> orderItems) {
		try {
			orderItemRepository.deleteAll(orderItems);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

}
