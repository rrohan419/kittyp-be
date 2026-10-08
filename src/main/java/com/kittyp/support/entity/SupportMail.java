package com.kittyp.support.entity;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * One inbound mail on a support conversation. The opening row and later replies share {@code supportId}.
 */
@Entity
@Table(name = "support_mail")
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
public class SupportMail extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Column(name = "support_id", nullable = false, length = 20)
	private String supportId;

	@Column(name = "sender_email", nullable = false, length = 254)
	private String senderEmail;

	@Column(nullable = false, length = 500)
	private String subject;

	@Column(nullable = false, length = 8000)
	private String body;

	@Column(name = "zoho_message_id", nullable = false, unique = true, length = 255)
	private String zohoMessageId;

	@Column(name = "thread_id", length = 255)
	private String threadId;

	@Column(nullable = false)
	private boolean opening;

	@Column(name = "ack_sent", nullable = false)
	private boolean ackSent;

	@Column(name = "user_id")
	private Long userId;
}
