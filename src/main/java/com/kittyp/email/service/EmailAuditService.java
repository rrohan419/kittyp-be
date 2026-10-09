/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import com.kittyp.email.dto.EmailAuditDto;

/**
 * @author rrohan419@gmail.com 
 */
public interface EmailAuditService {

	void saveEmailAudit(EmailAuditDto emailAuditDto);

	/**
	 * Checks the CPaaS authorization header, then applies a delivery or bounce event to the existing audit row.
	 */
	void receiveZeptoWebhook(String presentedSecret, String rawBody);
}
