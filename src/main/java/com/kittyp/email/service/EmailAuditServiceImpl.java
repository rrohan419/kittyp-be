/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.service;

import org.springframework.stereotype.Service;

import com.kittyp.common.util.Mapper;
import com.kittyp.email.dao.EmailAuditDao;
import com.kittyp.email.dto.EmailAuditDto;
import com.kittyp.email.entity.EmailAudit;
import com.kittyp.email.model.ZeptoWebhookEventRequest;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class EmailAuditServiceImpl implements EmailAuditService {

	private final EmailAuditDao emailAuditDao;
	private final Mapper mapper;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void saveEmailAudit(EmailAuditDto emailAuditDto) {
		EmailAudit emailAudit = mapper.convert(emailAuditDto, EmailAudit.class);
		
		emailAuditDao.save(emailAudit);

	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public void  zeptoWebhookEmailAudit(ZeptoWebhookEventRequest webhookEventRequest) {
//		EmailAudit savedAudit = emailAuditDao.emailAuditByRequestId(webhookEventRequest.getEventMessage().get(0).getRequestId());
		
//		if(savedAudit == null) {
			EmailAudit emailAudit = new EmailAudit();
			emailAudit.setWebhookRequestId(webhookEventRequest.getWebhookRequestId());
			emailAudit.setEventName(webhookEventRequest.getEventName().get(0));
			emailAudit.setMessage(webhookEventRequest.getEventName().get(0));
			emailAudit.setRecipientEmail(webhookEventRequest.getEventMessage().get(0).getEmailInfo().getTo().get(0).getEmailAddress().getAddress());
			emailAudit.setRequestId(webhookEventRequest.getEventMessage().get(0).getRequestId());
			emailAudit.setIpLocationInfo(webhookEventRequest.getEventMessage().get(0).getEventData().get(0).getDetails().get(0).getIpLocationInfo());
			emailAudit.setProvider("Zepto Mail");
			emailAuditDao.save(emailAudit);
//			return mapper.convert(savedAudit, EmailAuditModel.class);
//		}
		
//		savedAudit.setMessage(emailAuditDto.getMessage());
//		savedAudit.setMessageStatus(emailAuditDto.getMessageStatus());
//		emailAudit.setStatusCode(emailAuditDto.getStatusCode());
//		savedAudit.setEventName("email_sent");
//		
//		savedAudit = emailAuditDao.save(savedAudit);
//		
//		return mapper.convert(savedAudit, EmailAuditModel.class);
	}

}
