/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.model;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class CreateOrderModel {

	private String id;

	private String entity;

	private int amount;

	private int amount_paid;

	private int amount_due;

	private String currency;

	private String receipt;

	private String offer_id;

	private String status;

	private int attempts;

	private String notes;

	private long created_at;

}
