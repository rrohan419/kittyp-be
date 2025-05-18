/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.email.model.IpLocationInfo;

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
	
	@Column(nullable = false)
	private String provider;
	
	@Column(columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
	private IpLocationInfo ipLocationInfo;
}
