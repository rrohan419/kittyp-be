/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class NotificationModel {

	private String type;
	private String recipient;
	private String message;
	private String status;
}
