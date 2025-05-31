/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

/**
 * @author rrohan419@gmail.com 
 */
public interface InvoiceAuditService {

	void logFailure(String orderNumber, String reason, Throwable ex);
}
