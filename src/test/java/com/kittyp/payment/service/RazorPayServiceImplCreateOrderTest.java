package com.kittyp.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.payment.dto.RazorPayOrderRequestDto;
import com.kittyp.payment.enums.RazorpayOrderSource;
import com.kittyp.payment.model.CreateOrderModel;
import com.kittyp.product.service.ProductService;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

class RazorPayServiceImplCreateOrderTest {

	private OrderDao orderDao;
	private ProductService productService;
	private RazorpayGateway razorpayGateway;
	private ConsultationInvoiceRepository consultationInvoiceRepository;
	private ClinicService clinicService;
	private UserDao userDao;
	private RazorPayServiceImpl service;
	private User buyer;

	@BeforeEach
	void setUp() {
		orderDao = mock(OrderDao.class);
		productService = mock(ProductService.class);
		razorpayGateway = mock(RazorpayGateway.class);
		consultationInvoiceRepository = mock(ConsultationInvoiceRepository.class);
		clinicService = mock(ClinicService.class);
		userDao = mock(UserDao.class);
		TreatmentInvoiceService treatmentInvoiceService = new TreatmentInvoiceService(
				consultationInvoiceRepository, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null);

		service = new RazorPayServiceImpl(mock(Environment.class), orderDao, productService, razorpayGateway, null,
				consultationInvoiceRepository, treatmentInvoiceService, clinicService, userDao);

		buyer = User.builder().email("buyer@example.com").password("x").uuid("u-1").build();
		buyer.setId(9L);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("buyer@example.com", "n", List.of()));
		when(userDao.userByEmail("buyer@example.com")).thenReturn(buyer);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createStoreOrder_usesDbAmount_andIsIdempotent() {
		Order dbOrder = storeOrder(OrderStatus.CREATED, null);
		when(orderDao.orderByOrderNumber("KT-1")).thenReturn(dbOrder);

		CreateOrderModel created = new CreateOrderModel();
		created.setId("order_abc");
		created.setAmount(50000);
		when(razorpayGateway.createOrder(any(JSONObject.class))).thenReturn(created);

		RazorPayOrderRequestDto request = new RazorPayOrderRequestDto();
		request.setReceipt("KT-1");
		request.setAmount(new BigDecimal("1.00"));

		CreateOrderModel first = service.createOrder(request);
		assertEquals("order_abc", first.getId());

		org.mockito.ArgumentCaptor<JSONObject> captor = org.mockito.ArgumentCaptor.forClass(JSONObject.class);
		verify(razorpayGateway).createOrder(captor.capture());
		assertEquals(50000, captor.getValue().getInt("amount"));

		when(razorpayGateway.fetchOrder("order_abc")).thenReturn(created);

		CreateOrderModel second = service.createOrder(request);
		assertEquals("order_abc", second.getId());
		verify(razorpayGateway).createOrder(any(JSONObject.class));
		verify(razorpayGateway).fetchOrder("order_abc");
		verify(productService, never()).reserveStock(anyString(), anyInt(), anyString());
	}

	@Test
	void createStoreOrder_alreadyPaid_conflict() {
		when(orderDao.orderByOrderNumber("KT-1")).thenReturn(storeOrder(OrderStatus.SUCCESSFULL, "order_done"));
		RazorPayOrderRequestDto request = new RazorPayOrderRequestDto();
		request.setReceipt("KT-1");
		CustomException ex = assertThrows(CustomException.class, () -> service.createOrder(request));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
	}

	@Test
	void createInvoiceOrder_alreadyPaid_conflict() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-1")
				.amount(new BigDecimal("200.00"))
				.balance(BigDecimal.ZERO)
				.paymentStatus("PAID")
				.status(ConsultationInvoiceStatus.PAID)
				.doctor(buyer)
				.build();
		when(consultationInvoiceRepository.findByUuid("inv-1")).thenReturn(Optional.of(invoice));

		RazorPayOrderRequestDto request = new RazorPayOrderRequestDto();
		request.setInvoiceUuid("inv-1");
		request.setSource(RazorpayOrderSource.TREATMENT_INVOICE);

		CustomException ex = assertThrows(CustomException.class, () -> service.createOrder(request));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
		verify(razorpayGateway, never()).createOrder(any());
	}

	@Test
	void createInvoiceOrder_reusesPendingRazorpayId() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-1")
				.invoiceNumber("INV-2026-000001")
				.amount(new BigDecimal("200.00"))
				.balance(new BigDecimal("200.00"))
				.paymentStatus("UNPAID")
				.status(ConsultationInvoiceStatus.ISSUED)
				.razorpayOrderId("order_inv")
				.doctor(buyer)
				.build();
		when(consultationInvoiceRepository.findByUuid("inv-1")).thenReturn(Optional.of(invoice));
		CreateOrderModel existing = new CreateOrderModel();
		existing.setId("order_inv");
		when(razorpayGateway.fetchOrder("order_inv")).thenReturn(existing);

		RazorPayOrderRequestDto request = new RazorPayOrderRequestDto();
		request.setInvoiceUuid("inv-1");

		assertEquals("order_inv", service.createOrder(request).getId());
		verify(razorpayGateway, never()).createOrder(any());
	}

	private Order storeOrder(OrderStatus status, String aggregatorId) {
		Order order = Order.builder()
				.orderNumber("KT-1")
				.totalAmount(new BigDecimal("500.00"))
				.subTotal(new BigDecimal("500.00"))
				.currency(CurrencyType.INR)
				.status(status)
				.user(buyer)
				.orderItems(new ArrayList<>())
				.build();
		order.setAggregatorOrderNumber(aggregatorId);
		return order;
	}
}
