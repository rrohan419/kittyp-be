package com.kittyp.payment.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.service.TreatmentInvoiceService;
import com.kittyp.email.service.PaymentEventPublisher;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.product.service.ProductService;
import com.kittyp.user.entity.User;

class PaymentCaptureServiceTest {

	private OrderDao orderDao;
	private InvoiceService invoiceService;
	private ProductService productService;
	private PaymentEventPublisher paymentEventPublisher;
	private RecordingInvoiceService treatmentInvoiceService;
	private PaymentCaptureService service;

	@BeforeEach
	void setUp() {
		orderDao = mock(OrderDao.class);
		invoiceService = mock(InvoiceService.class);
		productService = mock(ProductService.class);
		paymentEventPublisher = new PaymentEventPublisher(mock(ApplicationEventPublisher.class));
		treatmentInvoiceService = new RecordingInvoiceService();
		service = new PaymentCaptureService(orderDao, invoiceService, productService, paymentEventPublisher,
				treatmentInvoiceService);
	}

	@Test
	void storeAlreadySuccessful_isNoOp() {
		User user = User.builder().email("a@b.com").password("x").uuid("u-1").build();
		Order order = Order.builder()
				.orderNumber("KT-1")
				.totalAmount(new BigDecimal("10.00"))
				.subTotal(new BigDecimal("10.00"))
				.status(OrderStatus.SUCCESSFULL)
				.user(user)
				.paymentId("pay_old")
				.build();
		order.setAggregatorOrderNumber("order_1");
		when(orderDao.orderByAggregatorOrderNumber("order_1")).thenReturn(order);

		service.completeCaptured("order_1", "pay_new");

		verify(productService, never()).confirmStockReservation("KT-1");
		verify(invoiceService, never()).generateInvoiceAndSaveInS3("KT-1", "u-1");
	}

	@Test
	void missingStoreOrder_delegatesToInvoice() {
		when(orderDao.orderByAggregatorOrderNumber("order_inv")).thenReturn(null);

		service.completeCaptured("order_inv", "pay_1");

		org.junit.jupiter.api.Assertions.assertEquals("order_inv", treatmentInvoiceService.razorpayOrderId);
		org.junit.jupiter.api.Assertions.assertEquals("pay_1", treatmentInvoiceService.paymentId);
	}

	static class RecordingInvoiceService extends TreatmentInvoiceService {
		String razorpayOrderId;
		String paymentId;

		RecordingInvoiceService() {
			super(mock(ConsultationInvoiceRepository.class), null, null, null, null, null, null, null, null, null,
					null, null, null, null, null);
		}

		@Override
		public ConsultationInvoice completeRazorpayCapture(String razorpayOrderId, String paymentId) {
			this.razorpayOrderId = razorpayOrderId;
			this.paymentId = paymentId;
			return ConsultationInvoice.builder().uuid("inv").build();
		}
	}
}
