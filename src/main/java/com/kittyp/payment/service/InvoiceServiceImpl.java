/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.user.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

	private final OrderDao orderDao;
	private final Environment env;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public void generateInvoice(String orderNumber) {
		Order order = orderDao.orderByOrderNumber(orderNumber);
		
		if(!order.getStatus().equals(OrderStatus.SUCCESSFULL)) {
			throw new CustomException(env.getProperty(ExceptionConstant.UNABLE_TO_GENARETE_INVOICE),
					HttpStatus.BAD_REQUEST);
		}
		User user = order.getUser();

	}

}
