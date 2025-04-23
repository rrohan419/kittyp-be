/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.payment.entity.WebhookEvent;

/**
 * @author rrohan419@gmail.com 
 */
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long>{

}
