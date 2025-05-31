/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.net.URL;

/**
 * @author rrohan419@gmail.com 
 */
public interface InvoiceService {

	void generateInvoiceAndSaveInS3(String orderNumber, String userUuid);
	
	URL getInvoicePresignedUrl(String orderNumber, String userUuid);
}
