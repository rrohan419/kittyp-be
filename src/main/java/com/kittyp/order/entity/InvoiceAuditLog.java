/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "invoice_audit_log")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceAuditLog extends BaseEntity{

    /**
	 * @author rrohan419@gmail.com
	 */
	private static final long serialVersionUID = 1L;

	private String orderNumber;
    
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;
}
