package com.kittyp.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.dto.ZohoMailRequest;
import com.kittyp.email.emailsender.ZeptoMailSender;
import com.kittyp.support.entity.SupportMail;
import com.kittyp.support.repository.SupportMailRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class SupportMailServiceTest {

	@Mock
	private SupportMailRepository supportMailRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ZeptoMailSender zeptoMailSender;

	@Mock
	private Environment environment;

	@Mock
	private EntityManager entityManager;

	@Mock
	private TransactionTemplate transactionTemplate;

	private SupportMailService service;

	@BeforeEach
	void setUp() {
		lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<Object> callback = invocation.getArgument(0);
			return callback.doInTransaction((TransactionStatus) null);
		});
		lenient().when(environment.getProperty(eq(AppConstant.KITTYP_MAIL_ID), anyString()))
				.thenReturn("noreply@kittyp.in");
		lenient().when(environment.getProperty(eq(AppConstant.KITTYP_SUPPORT_MAIL_ID), anyString()))
				.thenReturn("admin@kittyp.in");
		lenient().when(environment.getProperty(AppConstant.ZOHO_MAIL_WEBHOOK_SECRET)).thenReturn("top-secret");
		Query query = org.mockito.Mockito.mock(Query.class);
		lenient().when(query.getResultList()).thenReturn(List.of(0));
		lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		lenient().when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
		lenient().when(supportMailRepository.findByZohoMessageId(anyString())).thenReturn(Optional.empty());
		lenient().when(supportMailRepository.findFirstBySupportIdAndOpeningTrue(anyString()))
				.thenReturn(Optional.empty());
		lenient().when(supportMailRepository.findFirstByThreadId(anyString())).thenReturn(Optional.empty());
		lenient().when(supportMailRepository.maxOpeningSequence(anyString())).thenReturn(0);
		lenient().when(supportMailRepository.claimAck(any())).thenReturn(1);
		lenient().doAnswer(invocation -> {
			SupportMail row = invocation.getArgument(0);
			if (row.getId() == null) {
				row.setId(1L);
			}
			return row;
		}).when(supportMailRepository).saveAndFlush(any());
		service = new SupportMailService(supportMailRepository, userRepository, zeptoMailSender, environment,
				entityManager, transactionTemplate);
	}

	@Test
	void newSubjectCreatesOneIdAndOneAcknowledgement() {
		service.receive(mail("m-new", "visitor@example.com", "Need help", "x".repeat(8001), null));

		ArgumentCaptor<SupportMail> saved = ArgumentCaptor.forClass(SupportMail.class);
		verify(supportMailRepository).saveAndFlush(saved.capture());
		SupportMail row = saved.getValue();
		String expectedId = SupportIds.format(LocalDate.now(SupportIds.ZONE), 1);
		assertEquals(expectedId, row.getSupportId());
		assertTrue(row.isOpening());
		assertFalse(row.isAckSent());
		assertEquals("Need help", row.getSubject());
		assertEquals(8000, row.getBody().length());
		assertNull(row.getUserId());
		verify(entityManager).createNativeQuery(contains("pg_advisory_xact_lock(58291011)"));

		ArgumentCaptor<ZohoMailRequest> sent = ArgumentCaptor.forClass(ZohoMailRequest.class);
		verify(zeptoMailSender).sendHtml(sent.capture());
		verify(zeptoMailSender, never()).sendEmail(any());
		ZohoMailRequest ack = sent.getValue();
		assertEquals("[KittyP #" + expectedId + "] We received your request", ack.getSubject());
		assertEquals(expectedId, ack.getClientReference());
		assertNull(ack.getTemplateKey());
		assertEquals("noreply@kittyp.in", ack.getFrom().getAddress());
		assertEquals("visitor@example.com", ack.getTo().get(0).getEmailAddress().getAddress());
		assertTrue(ack.getHtmlBody().contains(expectedId));
		assertTrue(ack.getHtmlBody().contains("admin@kittyp.in"));
		verify(userRepository, never()).save(any());
		verify(userRepository, never()).saveAndFlush(any());
	}

	@Test
	void sameMessageTwiceStoresOneRowAndSendsOneAcknowledgement() {
		AtomicReference<SupportMail> stored = new AtomicReference<>();
		when(supportMailRepository.findByZohoMessageId("m-dup")).thenAnswer(invocation -> {
			SupportMail row = stored.get();
			return row == null ? Optional.empty() : Optional.of(row);
		});
		doAnswer(invocation -> {
			SupportMail row = invocation.getArgument(0);
			row.setId(11L);
			stored.set(row);
			return row;
		}).when(supportMailRepository).saveAndFlush(any());
		when(supportMailRepository.claimAck(11L)).thenAnswer(invocation -> {
			stored.get().setAckSent(true);
			return 1;
		});

		InboundMail incoming = mail("m-dup", "visitor@example.com", "Need help", "body", null);
		service.receive(incoming);
		service.receive(incoming);

		verify(supportMailRepository, times(1)).saveAndFlush(any());
		verify(zeptoMailSender, times(1)).sendHtml(any());
		verify(supportMailRepository, times(1)).claimAck(11L);
	}

	@Test
	void replyWithMatchingSenderStaysOnThatIdWithoutAcknowledgement() {
		SupportMail opening = opening("KIT-20200101-0007", "owner@example.com", "thread-1");
		when(supportMailRepository.findFirstBySupportIdAndOpeningTrue("KIT-20200101-0007"))
				.thenReturn(Optional.of(opening));

		service.receive(mail("reply-1", "owner@example.com",
				"Re: [KittyP #KIT-20200101-0007] We received your request", "thanks", null));

		SupportMail reply = savedRow();
		assertEquals("KIT-20200101-0007", reply.getSupportId());
		assertFalse(reply.isOpening());
		verify(zeptoMailSender, never()).sendHtml(any());
		verify(supportMailRepository, never()).claimAck(any());
		verify(entityManager, never()).createNativeQuery(anyString());
	}

	@Test
	void replyWithMatchingThreadStaysOnThatIdWithoutAcknowledgement() {
		SupportMail opening = opening("KIT-20200101-0007", "owner@example.com", "thread-1");
		when(supportMailRepository.findFirstBySupportIdAndOpeningTrue("KIT-20200101-0007"))
				.thenReturn(Optional.of(opening));

		service.receive(mail("reply-thread", "other@example.com",
				"Re: KIT-20200101-0007", "still here", "thread-1"));

		SupportMail reply = savedRow();
		assertEquals("KIT-20200101-0007", reply.getSupportId());
		assertFalse(reply.isOpening());
		verify(zeptoMailSender, never()).sendHtml(any());
	}

	@Test
	void threeRepliesStayOnTheOpeningId() {
		SupportMail opening = opening("KIT-20200101-0004", "owner@example.com", "thread-9");
		when(supportMailRepository.findFirstByThreadId("thread-9")).thenReturn(Optional.of(opening));
		List<SupportMail> replies = new ArrayList<>();
		doAnswer(invocation -> {
			SupportMail row = invocation.getArgument(0);
			row.setId((long) replies.size() + 30);
			replies.add(row);
			return row;
		}).when(supportMailRepository).saveAndFlush(any());

		for (int i = 1; i <= 3; i++) {
			service.receive(mail("multi-" + i, "owner@example.com", "Following up", "note " + i, "thread-9"));
		}

		assertEquals(3, replies.size());
		assertTrue(replies.stream().allMatch(row -> "KIT-20200101-0004".equals(row.getSupportId()) && !row.isOpening()));
		verify(zeptoMailSender, never()).sendHtml(any());
	}

	@Test
	void quotedIdFromAnotherSenderStartsANewConversation() {
		SupportMail opening = opening("KIT-20200101-0007", "owner@example.com", "thread-1");
		when(supportMailRepository.findFirstBySupportIdAndOpeningTrue("KIT-20200101-0007"))
				.thenReturn(Optional.of(opening));

		service.receive(mail("stranger-1", "stranger@example.com", "Please look at KIT-20200101-0007", "hi",
				"other-thread"));

		SupportMail created = savedRow();
		assertTrue(created.isOpening());
		assertNotEquals("KIT-20200101-0007", created.getSupportId());
		assertEquals(SupportIds.format(LocalDate.now(SupportIds.ZONE), 1), created.getSupportId());
		verify(zeptoMailSender).sendHtml(any());
	}

	@Test
	void unrelatedSubjectFromSameSenderCreatesANewId() {
		when(supportMailRepository.maxOpeningSequence(anyString())).thenReturn(3);

		service.receive(mail("fresh", "owner@example.com", "A different problem", "details", null));

		SupportMail created = savedRow();
		assertTrue(created.isOpening());
		assertEquals(SupportIds.format(LocalDate.now(SupportIds.ZONE), 4), created.getSupportId());
		verify(zeptoMailSender).sendHtml(any());
	}

	@Test
	void uniqueOpeningConflictRetriesWithTheNextId() {
		AtomicInteger attempts = new AtomicInteger();
		when(supportMailRepository.maxOpeningSequence(anyString())).thenReturn(0, 1);
		doAnswer(invocation -> {
			SupportMail row = invocation.getArgument(0);
			if (attempts.incrementAndGet() == 1) {
				throw new DataIntegrityViolationException("opening");
			}
			row.setId(2L);
			return row;
		}).when(supportMailRepository).saveAndFlush(any());

		service.receive(mail("race-1", "visitor@example.com", "Help", "body", null));

		ArgumentCaptor<SupportMail> saved = ArgumentCaptor.forClass(SupportMail.class);
		verify(supportMailRepository, times(2)).saveAndFlush(saved.capture());
		assertEquals(SupportIds.format(LocalDate.now(SupportIds.ZONE), 2),
				saved.getAllValues().get(1).getSupportId());
		verify(entityManager, times(2)).createNativeQuery(contains("pg_advisory_xact_lock(58291011)"));
		verify(zeptoMailSender).sendHtml(any());
	}

	@Test
	void twoConcurrentCreatesGetTwoIds() throws Exception {
		Object mutex = new Object();
		AtomicInteger sequence = new AtomicInteger();
		Set<String> openingIds = ConcurrentHashMap.newKeySet();
		when(supportMailRepository.maxOpeningSequence(anyString())).thenAnswer(invocation -> {
			synchronized (mutex) {
				return sequence.get();
			}
		});
		doAnswer(invocation -> {
			SupportMail row = invocation.getArgument(0);
			synchronized (mutex) {
				if (row.isOpening() && !openingIds.add(row.getSupportId())) {
					throw new DataIntegrityViolationException("opening");
				}
				sequence.set(Math.max(sequence.get(), Integer.parseInt(row.getSupportId().substring(13))));
				row.setId((long) openingIds.size());
				return row;
			}
		}).when(supportMailRepository).saveAndFlush(any());

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Void> first = pool.submit(() -> {
				start.await();
				service.receive(mail("c-1", "a@example.com", "One", "body", null));
				return null;
			});
			Future<Void> second = pool.submit(() -> {
				start.await();
				service.receive(mail("c-2", "b@example.com", "Two", "body", null));
				return null;
			});
			start.countDown();
			first.get(10, TimeUnit.SECONDS);
			second.get(10, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}

		assertEquals(2, openingIds.size());
		verify(zeptoMailSender, times(2)).sendHtml(any());
	}

	@Test
	void knownEmailLinksUserAndUnknownEmailDoesNotCreateOne() {
		User user = new User();
		user.setId(9L);
		when(userRepository.findByEmailIgnoreCase("known@kittyp.in")).thenReturn(Optional.of(user));

		service.receive(mail("known-1", "known@kittyp.in", "Account", "body", null));
		assertEquals(9L, savedRow().getUserId());

		service.receive(mail("unknown-1", "unknown@example.com", "Who", "body", null));
		ArgumentCaptor<SupportMail> rows = ArgumentCaptor.forClass(SupportMail.class);
		verify(supportMailRepository, times(2)).saveAndFlush(rows.capture());
		assertNull(rows.getAllValues().get(1).getUserId());
		verify(userRepository, never()).save(any());
		verify(userRepository, never()).saveAndFlush(any());
	}

	@Test
	void missingOrWrongSecretIsRejected() {
		CustomException missing = assertThrows(CustomException.class, () -> service.verifySecret(null));
		assertEquals(HttpStatus.UNAUTHORIZED, missing.getHttpStatus());
		CustomException blank = assertThrows(CustomException.class, () -> service.verifySecret("  "));
		assertEquals(HttpStatus.UNAUTHORIZED, blank.getHttpStatus());
		CustomException wrong = assertThrows(CustomException.class, () -> service.verifySecret("nope"));
		assertEquals(HttpStatus.UNAUTHORIZED, wrong.getHttpStatus());

		when(environment.getProperty(AppConstant.ZOHO_MAIL_WEBHOOK_SECRET)).thenReturn(" ");
		CustomException unset = assertThrows(CustomException.class, () -> service.verifySecret("top-secret"));
		assertEquals(HttpStatus.UNAUTHORIZED, unset.getHttpStatus());

		when(environment.getProperty(AppConstant.ZOHO_MAIL_WEBHOOK_SECRET)).thenReturn("top-secret");
		service.verifySecret("top-secret");
	}

	@Test
	void acknowledgementSenderDoesNotOpenATicketButAdminCan() {
		service.receive(mail("loop", "NoReply@kittyp.in", "Re: [KittyP #KIT-20200101-0007]", "ack body", "thread-1"));
		verify(supportMailRepository, never()).saveAndFlush(any());
		verify(zeptoMailSender, never()).sendHtml(any());

		service.receive(mail("admin-1", "admin@kittyp.in", "Customer wrote in", "please check", null));
		SupportMail created = savedRow();
		assertEquals("admin@kittyp.in", created.getSenderEmail());
		assertTrue(created.isOpening());
		verify(zeptoMailSender).sendHtml(any());
	}

	@Test
	void zeptoFailureReleasesTheClaimAndReturnsBadGateway() {
		when(zeptoMailSender.sendHtml(any())).thenThrow(new IllegalStateException("down"));

		CustomException failure = assertThrows(CustomException.class,
				() -> service.receive(mail("m-fail", "visitor@example.com", "Help", "body", null)));

		assertEquals(HttpStatus.BAD_GATEWAY, failure.getHttpStatus());
		verify(supportMailRepository).claimAck(1L);
		verify(supportMailRepository).releaseAck(1L);
	}

	@Test
	void parserReadsWrappedPayloadAndRejectsBadInput() {
		SupportMailParser parser = new SupportMailParser(new ObjectMapper());
		InboundMail parsed = parser.parse("""
				{"data":{"fromAddress":"Ada <ada@example.com>","subject":"Hi","messageId":42,"thread_id":"t-1","summary":"hello"}}
				""");
		assertEquals("42", parsed.messageId());
		assertEquals("ada@example.com", parsed.from());
		assertEquals("Hi", parsed.subject());
		assertEquals("hello", parsed.body());
		assertEquals("t-1", parsed.threadId());

		CustomException invalid = assertThrows(CustomException.class, () -> parser.parse("{"));
		assertEquals(HttpStatus.BAD_REQUEST, invalid.getHttpStatus());
		CustomException missing = assertThrows(CustomException.class,
				() -> parser.parse("{\"fromAddress\":\"a@b.com\"}"));
		assertEquals(HttpStatus.BAD_REQUEST, missing.getHttpStatus());
	}

	private SupportMail savedRow() {
		ArgumentCaptor<SupportMail> saved = ArgumentCaptor.forClass(SupportMail.class);
		verify(supportMailRepository).saveAndFlush(saved.capture());
		return saved.getValue();
	}

	private static SupportMail opening(String supportId, String sender, String threadId) {
		SupportMail row = new SupportMail();
		row.setId(7L);
		row.setSupportId(supportId);
		row.setSenderEmail(sender);
		row.setSubject("Need help");
		row.setBody("body");
		row.setZohoMessageId("opening-message");
		row.setThreadId(threadId);
		row.setOpening(true);
		row.setAckSent(true);
		row.setIsActive(true);
		return row;
	}

	private static InboundMail mail(String messageId, String from, String subject, String body, String threadId) {
		return new InboundMail(messageId, from, subject, body, threadId);
	}
}
