/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.order.entity.InvoiceAuditLog;

/**
 * @author rrohan419@gmail.com 
 */
public interface InvoiceAuditLogRepository extends JpaRepository<InvoiceAuditLog, Long>{

}
