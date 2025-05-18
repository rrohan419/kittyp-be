/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class EmailAuditDto {

	private String statusCode;
	
	private String message;
	
	private String requestId;
	
	private String messageStatus;
	
	private String recipientEmail;
	
	private String provider;
	
	private String eventName;
}
