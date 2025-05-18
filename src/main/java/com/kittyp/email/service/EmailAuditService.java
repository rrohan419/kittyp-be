/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import com.kittyp.email.dto.EmailAuditDto;
import com.kittyp.email.model.ZeptoWebhookEventRequest;

/**
 * @author rrohan419@gmail.com 
 */
public interface EmailAuditService {

	
	void saveEmailAudit(EmailAuditDto emailAuditDto);
	
	void zeptoWebhookEmailAudit(ZeptoWebhookEventRequest webhookEventRequest);
}
