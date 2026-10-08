package com.kittyp.support.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.support.entity.SupportMail;
import com.kittyp.support.repository.SupportMailRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores inbound mail on {@code support_mail} and sends one acknowledgement for a new conversation.
 *
 * <p>Not exactly-once. The opening row is committed before Zepto is called. The acknowledgement is claimed with
 * {@code ack_sent=true} and then sent. If Zepto throws, the claim is cleared and the webhook returns 500 so Zoho
 * retries. If the process dies after that claim commits and before Zepto accepts the message, a later delivery of
 * the same message will not send again. If Zepto accepts the message and the claim update does not stick, a retry
 * can send a second acknowledgement. No queue is added to close that window.
 */
@Slf4j
@Service
public class SupportMailService {

	private final SupportMailRepository supportMailRepository;
	private final UserRepository userRepository;
	private final ZeptoMailSender zeptoMailSender;
	private final Environment environment;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	@Autowired
	public SupportMailService(SupportMailRepository supportMailRepository, UserRepository userRepository,
			ZeptoMailSender zeptoMailSender, Environment environment, EntityManager entityManager,
			PlatformTransactionManager transactionManager) {
		this(supportMailRepository, userRepository, zeptoMailSender, environment, entityManager,
				new TransactionTemplate(transactionManager));
	}

	SupportMailService(SupportMailRepository supportMailRepository, UserRepository userRepository,
			ZeptoMailSender zeptoMailSender, Environment environment, EntityManager entityManager,
			TransactionTemplate transactionTemplate) {
		this.supportMailRepository = supportMailRepository;
		this.userRepository = userRepository;
		this.zeptoMailSender = zeptoMailSender;
		this.environment = environment;
		this.entityManager = entityManager;
		this.transactionTemplate = transactionTemplate;
	}

	public void verifySecret(String presented) {
		String expected = environment.getProperty(AppConstant.ZEPTOMAIL_WEBHOOK_SECRET);
		if (expected == null || expected.isBlank() || presented == null || presented.isBlank()) {
			throw new CustomException("Missing webhook secret", HttpStatus.UNAUTHORIZED);
		}
		byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
		byte[] required = expected.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(actual, required)) {
			throw new CustomException("Invalid webhook secret", HttpStatus.UNAUTHORIZED);
		}
	}

	private static byte[] hmacSha256(String secret, byte[] rawBody) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(rawBody);
		} catch (GeneralSecurityException ex) {
			throw new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
		}
	}

	private static boolean isHeaderSafe(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c <= 0x1F || c == 0x7F) {
				return false;
			}
		}
		return true;
	}

	public String receive(InboundMail mail) {
		if (isOwnAcknowledgement(mail.from())) {
			log.info("Skipped inbound mail from the acknowledgement sender");
			return null;
		}
		StoredMail stored = writeWithRetry(mail);
		if (stored.shouldAcknowledge()) {
			acknowledge(stored.mail(), mail.senderName());
		}
		return stored.mail() == null ? null : stored.mail().getSupportId();
	}

	private StoredMail writeWithRetry(InboundMail mail) {
		try {
			return transactionTemplate.execute(status -> write(mail));
		} catch (DataIntegrityViolationException ex) {
			log.info("Retrying support mail {} after a unique constraint conflict", mail.messageId());
			return transactionTemplate.execute(status -> write(mail));
		}
	}

	private StoredMail write(InboundMail mail) {
		Optional<SupportMail> existing = supportMailRepository.findByZohoMessageId(mail.messageId());
		if (existing.isPresent()) {
			return StoredMail.of(existing.get());
		}
		SupportMail conversation = matchConversation(mail);
		if (conversation != null) {
			SupportMail reply = insert(newRow(conversation.getSupportId(), mail, false));
			log.info("Continued support {} for message {}", reply.getSupportId(), mail.messageId());
			return StoredMail.of(reply);
		}
		lockOpeningIds();
		SupportMail opening = insert(newOpening(mail));
		log.info("Opened support {} for message {}", opening.getSupportId(), mail.messageId());
		return StoredMail.of(opening);
	}

	private SupportMail matchConversation(InboundMail mail) {
		String subjectId = SupportIds.findInSubject(mail.subject());
		if (subjectId != null) {
			Optional<SupportMail> opening = supportMailRepository.findFirstBySupportIdAndOpeningTrue(subjectId);
			if (opening.isPresent() && associated(opening.get(), mail)) {
				return opening.get();
			}
		}
		String threadId = blankToNull(mail.threadId());
		if (threadId == null) {
			return null;
		}
		return supportMailRepository.findFirstByThreadId(threadId).orElse(null);
	}

	/**
	 * A quoted support id continues that conversation only when the sender or the Zoho thread matches it.
	 * Sender address alone never selects a conversation.
	 */
	private static boolean associated(SupportMail opening, InboundMail mail) {
		boolean sameSender = opening.getSenderEmail() != null
				&& opening.getSenderEmail().equalsIgnoreCase(mail.from());
		String incomingThread = blankToNull(mail.threadId());
		boolean sameThread = incomingThread != null && incomingThread.equals(opening.getThreadId());
		return sameSender || sameThread;
	}

	private SupportMail newOpening(InboundMail mail) {
		LocalDate today = LocalDate.now(SupportIds.ZONE);
		Number max = supportMailRepository.maxOpeningSequence(SupportIds.prefix(today));
		int sequence = max == null ? 0 : max.intValue();
		if (sequence >= 99999) {
			throw new CustomException("Support id sequence exhausted", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return newRow(SupportIds.format(today, sequence + 1), mail, true);
	}

	private SupportMail newRow(String supportId, InboundMail mail, boolean opening) {
		SupportMail row = new SupportMail();
		row.setSupportId(supportId);
		row.setSenderEmail(mail.from().trim());
		row.setSubject(cap(mail.subject(), 500));
		row.setBody(cap(mail.body(), 8000));
		row.setZohoMessageId(mail.messageId());
		row.setThreadId(blankToNull(mail.threadId()));
		row.setOpening(opening);
		row.setAckSent(false);
		row.setUserId(userIdFor(mail.from()));
		row.setIsActive(true);
		return row;
	}

	private Long userIdFor(String email) {
		return userRepository.findByEmailIgnoreCase(email.trim()).map(User::getId).orElse(null);
	}

	private SupportMail insert(SupportMail row) {
		return supportMailRepository.saveAndFlush(row);
	}

	private void lockOpeningIds() {
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(" + SupportIds.ADVISORY_LOCK_KEY + ")")
				.getResultList();
	}

	private void acknowledge(SupportMail mail, String senderName) {
		String templateKey = environment.getProperty(TemplateConstant.ZOHO_CONTACT_ACK_EMAIL_TEMPLATE_ID);
		if (templateKey == null || templateKey.isBlank()) {
			throw new CustomException("Mail is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
		String name = senderName == null || senderName.isBlank() ? mail.getSenderEmail() : senderName.trim();
		ZeptoMailDto ack = new ZeptoMailDto();
		ack.setTemplateKey(templateKey.trim());
		String subject = mail.getSubject() == null ? "" : mail.getSubject();
		ack.setRecipientEmail(mail.getSenderEmail());
		ack.setRecipientName(name);
		ack.setReplyToEmail(supportInbox());
		ack.setReplyToName("Kittyp support");
		ack.setMergeInfo(Map.of(
				"Customer_Name", name,
				"Subject", subject,
				"Support_Id", mail.getSupportId(),
				"Support_Email", supportInbox()));
		Long id = mail.getId();
		Integer claimed = transactionTemplate.execute(status -> supportMailRepository.claimAck(id));
		if (claimed == null || claimed == 0) {
			return;
		}
		try {
			zeptoMailSender.sendEmail(ack);
			log.info("Sent support acknowledgement for {}", mail.getSupportId());
		} catch (RuntimeException ex) {
			transactionTemplate.execute(status -> {
				supportMailRepository.releaseAck(id);
				return null;
			});
			log.warn("Support acknowledgement failed for {}", mail.getSupportId());
			throw new CustomException("Could not send support acknowledgement", HttpStatus.BAD_GATEWAY);
		}
	}

	private boolean isOwnAcknowledgement(String from) {
		return from != null && from.equalsIgnoreCase(fromAddress());
	}

	private String fromAddress() {
		String configured = environment.getProperty(AppConstant.KITTYP_MAIL_ID, "noreply@kittyp.in");
		if (configured == null || configured.isBlank()) {
			return "noreply@kittyp.in";
		}
		return configured.trim();
	}

	private String supportInbox() {
		String configured = environment.getProperty(AppConstant.KITTYP_SUPPORT_MAIL_ID, "support@kittyp.in");
		if (configured == null || configured.isBlank()) {
			return "support@kittyp.in";
		}
		return configured.trim();
	}

	private static String cap(String value, int max) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.length() <= max) {
			return trimmed;
		}
		return trimmed.substring(0, max);
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
	}
}
