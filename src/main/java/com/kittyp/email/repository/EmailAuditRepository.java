/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.email.entity.EmailAudit;

/**
 * @author rrohan419@gmail.com 
 */
public interface EmailAuditRepository extends JpaRepository<EmailAudit, Long> {

	EmailAudit findByRequestId(String requestId);
}
