/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.payment.enums.WebhookSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "webhook_event")
@EqualsAndHashCode(callSuper = false)
@Data
public class WebhookEvent extends BaseEntity{

	private static final long serialVersionUID = 1L;
	
	private WebhookSource source;         // e.g., RAZORPAY, STRIPE
    private String eventType;      // e.g., payment.authorized
    private String paymentId;     // e.g., pay_QMNAvB0Ftv5yW1
    private String orderId;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;        // full raw payload
    
    private String status;         // RECEIVED, PROCESSED, FAILED
    private String errorMessage;
    private Integer retryCount;
}
