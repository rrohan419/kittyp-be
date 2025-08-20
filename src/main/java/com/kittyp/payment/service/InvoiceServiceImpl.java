package com.kittyp.payment.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.service.S3StorageService;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.order.service.InvoiceAuditService;
import com.kittyp.payment.model.InvoiceData;
import com.kittyp.payment.util.PdfGenerator;
import com.kittyp.user.dao.UserDao;

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
	private final InvoiceAuditService invoiceAuditService;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void generateInvoiceAndSaveInS3(String orderNumber, String userUuid) {
		Order order = orderDao.orderByOrderNumber(orderNumber);

		if (!order.getStatus().equals(OrderStatus.SUCCESSFULL)) {
			throw new CustomException(env.getProperty(ExceptionConstant.UNABLE_TO_GENARETE_INVOICE),
					HttpStatus.BAD_REQUEST);
		}
		try {
			InvoiceData invoiceData = new InvoiceData().from(order);

			byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoiceData);
			String orderId = orderNumber + "-" + userUuid;
			s3Service.uploadInvoice(orderId, pdfBytes);
			log.info("Successfully generating invoice for order number : " + orderNumber);
		} catch (Exception e) {
			invoiceAuditService.logFailure(orderNumber, e.getMessage(), e);
			log.error("Error generating invoice for order number : " + orderNumber + " with message ====> "
					+ e.getMessage());
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

		if (userUuid == null || userUuid.isBlank()) {
			String email = SecurityContextHolder.getContext().getAuthentication().getName();
			userUuid = userDao.userByEmail(email).getUuid();
		}

		if (!order.getUser().getUuid().equals(userUuid)) {
			throw new CustomException("Not Authorized to access this invoice", HttpStatus.FORBIDDEN);
		}
		String orderId = orderNumber + "-" + userUuid;
		return s3Service.presignedInvoiceUrl(orderId, Duration.ofMinutes(10L));

	}

}
