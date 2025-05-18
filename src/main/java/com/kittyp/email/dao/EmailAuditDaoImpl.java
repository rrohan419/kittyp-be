/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dao;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.entity.EmailAudit;
import com.kittyp.email.repository.EmailAuditRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Repository
@RequiredArgsConstructor
public class EmailAuditDaoImpl implements EmailAuditDao {

	private final EmailAuditRepository emailAuditRepository;
	private final Environment env;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public EmailAudit save(EmailAudit emailAudit) {
		try {
			return emailAuditRepository.save(emailAudit);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public EmailAudit emailAuditByRequestId(String requestId) {
		try {
			return emailAuditRepository.findByRequestId(requestId);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
