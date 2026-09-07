package com.kittyp.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.entity.Order;
import com.kittyp.payment.constants.RazorPayConstant;
import com.kittyp.payment.dto.RazorpayVerificationRequest;
import com.kittyp.product.service.ProductService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

class RazorPayServiceImplVerifyPaymentTest {

	private static final String SECRET = "test_razorpay_secret";

	private OrderDao orderDao;
	private RazorpayGateway razorpayGateway;
	private CaptureProbe captureProbe;
	private RazorPayServiceImpl service;

	@BeforeEach
	void setUp() throws Exception {
		orderDao = mock(OrderDao.class);
		razorpayGateway = mock(RazorpayGateway.class);
		captureProbe = new CaptureProbe();
		Environment env = mock(Environment.class);
		when(env.getProperty(RazorPayConstant.KEY_SECRET)).thenReturn(SECRET);

		TreatmentInvoiceService treatmentInvoiceService = new TreatmentInvoiceService(
				mock(ConsultationInvoiceRepository.class), null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null);

		service = new RazorPayServiceImpl(env, orderDao, mock(ProductService.class), razorpayGateway, captureProbe,
				mock(ConsultationInvoiceRepository.class), treatmentInvoiceService, mock(ClinicService.class),
				mock(UserDao.class));

		User buyer = User.builder().email("buyer@example.com").password("x").uuid("u-1").build();
		buyer.setId(9L);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("buyer@example.com", "n", List.of()));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void verifyPayment_invalidSignature_rejected() {
		RazorpayVerificationRequest request = verification("order_1", "pay_1", "bad_sig");

		CustomException ex = assertThrows(CustomException.class, () -> service.verifyPayment(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("Invalid payment signature", ex.getMessage());
		assertNull(captureProbe.orderId);
	}

	@Test
	void verifyPayment_amountMismatch_rejected() throws Exception {
		RazorpayVerificationRequest request = verification("order_1", "pay_1", sign("order_1", "pay_1"));
		Order order = Order.builder().orderNumber("KP-1").totalAmount(new BigDecimal("500.00"))
				.currency(CurrencyType.INR).build();
		when(orderDao.orderByAggregatorOrderNumber("order_1")).thenReturn(order);

		JSONObject payment = new JSONObject();
		payment.put("order_id", "order_1");
		payment.put("amount", 100);
		payment.put("currency", "INR");
		when(razorpayGateway.fetchPayment("pay_1")).thenReturn(payment);

		CustomException ex = assertThrows(CustomException.class, () -> service.verifyPayment(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("Payment amount does not match order", ex.getMessage());
		assertNull(captureProbe.orderId);
	}

	@Test
	void verifyPayment_signatureAndAmountOk_captures() throws Exception {
		RazorpayVerificationRequest request = verification("order_1", "pay_1", sign("order_1", "pay_1"));
		Order order = Order.builder().orderNumber("KP-1").totalAmount(new BigDecimal("200.00"))
				.currency(CurrencyType.INR).build();
		when(orderDao.orderByAggregatorOrderNumber("order_1")).thenReturn(order);

		JSONObject payment = new JSONObject();
		payment.put("order_id", "order_1");
		payment.put("amount", 20000);
		payment.put("currency", "INR");
		when(razorpayGateway.fetchPayment("pay_1")).thenReturn(payment);

		assertEquals("Payment verified successfully", service.verifyPayment(request));
		assertEquals("order_1", captureProbe.orderId);
		assertEquals("pay_1", captureProbe.paymentId);
	}

	private static RazorpayVerificationRequest verification(String orderId, String paymentId, String signature) {
		RazorpayVerificationRequest request = new RazorpayVerificationRequest();
		request.setOrderId(orderId);
		request.setPaymentId(paymentId);
		request.setSignature(signature);
		return request;
	}

	private static String sign(String orderId, String paymentId) throws Exception {
		String payload = orderId + "|" + paymentId;
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	static class CaptureProbe extends PaymentCaptureService {
		String orderId;
		String paymentId;

		CaptureProbe() {
			super(null, null, null, null, null);
		}

		@Override
		public void completeCaptured(String razorpayOrderId, String paymentId) {
			this.orderId = razorpayOrderId;
			this.paymentId = paymentId;
		}
	}
}
