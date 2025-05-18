/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dao;

import com.kittyp.email.entity.EmailAudit;

/**
 * @author rrohan419@gmail.com 
 */
public interface EmailAuditDao {

	EmailAudit save(EmailAudit emailAudit);
	
	EmailAudit emailAuditByRequestId(String requestId);
}
