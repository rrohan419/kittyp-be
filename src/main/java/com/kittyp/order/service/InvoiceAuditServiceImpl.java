/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.order.entity.InvoiceAuditLog;
import com.kittyp.order.repository.InvoiceAuditLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class InvoiceAuditServiceImpl implements InvoiceAuditService {

	private final InvoiceAuditLogRepository invoiceAuditLogRepository;
	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void logFailure(String orderNumber, String reason, Throwable ex) {
		InvoiceAuditLog log = new InvoiceAuditLog();
        log.setOrderNumber(orderNumber);
        log.setFailureReason(reason);
        log.setStackTrace(ExceptionUtils.getStackTrace(ex));
        invoiceAuditLogRepository.save(log);

	}

}
