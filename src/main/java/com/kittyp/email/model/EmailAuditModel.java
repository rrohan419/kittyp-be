/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import lombok.Data;

/**
 * @author rrohan419@gmail.com
 */
@Data
public class EmailAuditModel {

	private Long id;
	
	private String statusCode;

	private String message;

	private String requestId;

	private String messageStatus;

	private String recipientEmail;

	private String messageType;
}
