/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import com.kittyp.order.entity.Order;

/**
 * @author rrohan419@gmail.com 
 */
public interface InvoiceService {

	void generateInvoice(String orderNumber);
}
