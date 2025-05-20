package com.kittyp.payment.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.service.S3StorageService;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.payment.model.InvoiceData;
import com.kittyp.payment.util.PdfGenerator;
import com.kittyp.user.dao.UserDao;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

	private final OrderDao orderDao;
	private final Environment env;
	private final PdfGenerator pdfGenerator;
	private final S3StorageService s3Service;
	private final UserDao userDao;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Async
	@Transactional
	@Override
	public void generateInvoiceAndSaveInS3(String orderNumber) {
		Order order = orderDao.orderByOrderNumber(orderNumber);

		if (!order.getStatus().equals(OrderStatus.SUCCESSFULL)) {
			throw new CustomException(env.getProperty(ExceptionConstant.UNABLE_TO_GENARETE_INVOICE),
					HttpStatus.BAD_REQUEST);
		}
		try {
			InvoiceData invoiceData = new InvoiceData().from(order);

			byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoiceData);

			s3Service.uploadInvoice(orderNumber, pdfBytes);
		
		} catch (Exception e) {
			log.error("Error generating invoice for order number : "+orderNumber+" with message ====> "+e.getMessage());
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public URL getInvoicePresignedUrl(String orderNumber, String userUuid) {
		Order order = orderDao.orderByOrderNumber(orderNumber);

		if (!order.getStatus().equals(OrderStatus.SUCCESSFULL)) {
			throw new CustomException(env.getProperty(ExceptionConstant.UNABLE_TO_GENARETE_INVOICE),
					HttpStatus.BAD_REQUEST);
		}
		
		if(userUuid == null || userUuid.isBlank()) {
			String email = SecurityContextHolder.getContext().getAuthentication().getName();
			userUuid = userDao.userByEmail(email).getUuid();
		}
		
		if(!order.getUser().getUuid().equals(userUuid)) {
			throw new CustomException("Not Authorized to access this invoice",
					HttpStatus.FORBIDDEN);
		}
		
		return s3Service.presignedUrl(orderNumber, Duration.ofMinutes(10L));
		
	}
}
