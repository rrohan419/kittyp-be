package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.kittyp.email.service.ZeptoMailService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class HardenedSmsGatewayServiceTest {

	private MockWebServer server;
	private ListAppender<ILoggingEvent> appender;
	private Logger smsLogger;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
		smsLogger = (Logger) LoggerFactory.getLogger(SmsGatewayService.class);
		appender = new ListAppender<>();
		appender.start();
		smsLogger.addAppender(appender);
	}

	@AfterEach
	void tearDown() throws Exception {
		smsLogger.detachAppender(appender);
		server.shutdown();
	}

	@Test
	void sendOtp_readTimeout_failsoverToZeptoOnce() {
		server.enqueue(new MockResponse().setHeadersDelay(6, TimeUnit.SECONDS).setBody("{\"id\":\"late\"}"));
		ZeptoMailService zepto = mock(ZeptoMailService.class);
		SmsGatewayService service = newService(zepto);

		service.sendOtp("+919876543210", "654321", "user@kittyp.test");

		verify(zepto, times(1)).sendEmailChangeOtp("user@kittyp.test", "there", "KittyP", "654321");
		assertEquals(1, server.getRequestCount());
		assertNoRawOtp();
	}

	@Test
	void sendOtp_shortPhone_doesNotCallGateway() {
		ZeptoMailService zepto = mock(ZeptoMailService.class);
		SmsGatewayService service = newService(zepto);

		assertThrows(IllegalArgumentException.class, () -> service.sendOtp("12345", "654321", "user@kittyp.test"));
		assertEquals(0, server.getRequestCount());
	}

	@Test
	void sendOtp_crlfPhone_doesNotCallGateway() {
		ZeptoMailService zepto = mock(ZeptoMailService.class);
		SmsGatewayService service = newService(zepto);

		assertThrows(IllegalArgumentException.class,
				() -> service.sendOtp("+919876543210\r\nInject", "654321", "user@kittyp.test"));
		assertEquals(0, server.getRequestCount());
	}

	@Test
	void sendOtp_logsDoNotContainRawOtp() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"m1\"}"));
		SmsGatewayService service = newService(mock(ZeptoMailService.class));

		service.sendOtp("+919876543210", "654321", "user@kittyp.test");
		server.takeRequest(2, TimeUnit.SECONDS);
		assertNoRawOtp();
	}

	private SmsGatewayService newService(ZeptoMailService zepto) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(3000)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofMillis(5000));
		RestClient restClient = RestClient.builder().requestFactory(factory).build();
		String base = server.url("/").toString().replaceAll("/$", "");
		return new SmsGatewayService(restClient, zepto, base, "sms", "secret");
	}

	private void assertNoRawOtp() {
		String joined = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
				.reduce("", (left, right) -> left + " " + right);
		assertTrue(!joined.contains("654321"), joined);
	}
}
