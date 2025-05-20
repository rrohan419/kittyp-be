/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

/**
 * @author rrohan419@gmail.com 
 */
public interface InvoiceService {

	void generateInvoiceAndSaveInS3(String orderNumber);
}
