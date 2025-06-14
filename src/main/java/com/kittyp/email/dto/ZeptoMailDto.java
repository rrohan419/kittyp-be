/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
@Setter
public class ZeptoMailDto implements IEmailDto{

	private String templateKey;
	private String type;
	private String recipientName;
	private String recipientEmail;
	private String message;
//	private String provider;
	private Map<String, Object> mergeInfo;
}
