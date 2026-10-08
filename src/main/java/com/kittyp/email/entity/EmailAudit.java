/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.entity;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "email_audits")
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAudit extends BaseEntity{

	private static final long serialVersionUID = 1L;

	@Column
	private String webhookRequestId;
	
	@Column(nullable = false)
	private String eventName;
	
	@Column
	private String message;
	
	@Column(nullable = false)
	private String requestId;
	
	@Column
	private String recipientEmail;
}
