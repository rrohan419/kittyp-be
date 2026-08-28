/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.payment.entity.WebhookEvent;

/**
 * @author rrohan419@gmail.com 
 */
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

	Optional<WebhookEvent> findByPaymentIdAndEventType(String paymentId, String eventType);
}
