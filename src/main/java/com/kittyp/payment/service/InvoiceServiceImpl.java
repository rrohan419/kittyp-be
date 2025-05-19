/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.payment.model.InvoiceData;
import com.kittyp.payment.util.PdfGenerator;

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
	private final PdfGenerator pdfGenerator;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public void generateInvoice(String orderNumber) {
		Order order = orderDao.orderByOrderNumber(orderNumber);

		if (!order.getStatus().equals(OrderStatus.SUCCESSFULL)) {
			throw new CustomException(env.getProperty(ExceptionConstant.UNABLE_TO_GENARETE_INVOICE),
					HttpStatus.BAD_REQUEST);
		}
		try {
			InvoiceData invoiceData = new InvoiceData().from(order);

			byte[] pdfBytes = pdfGenerator.generateInvoicePdf(invoiceData);

			Files.write(Paths.get("invoice.pdf"), pdfBytes);
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
		}
	}
}
