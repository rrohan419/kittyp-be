package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;

@ExtendWith(MockitoExtension.class)
class TreatmentInvoiceRazorpayCaptureTest {

	@Mock
	private ConsultationInvoiceRepository invoiceRepository;

	@InjectMocks
	private TreatmentInvoiceService service;

	@Test
	void completeRazorpayCapture_marksPaid() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-1")
				.amount(new BigDecimal("250.00"))
				.paidAmount(BigDecimal.ZERO)
				.balance(new BigDecimal("250.00"))
				.paymentStatus("UNPAID")
				.status(ConsultationInvoiceStatus.ISSUED)
				.razorpayOrderId("order_inv")
				.build();
		when(invoiceRepository.findByRazorpayOrderId("order_inv")).thenReturn(Optional.of(invoice));
		when(invoiceRepository.save(any(ConsultationInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

		ConsultationInvoice paid = service.completeRazorpayCapture("order_inv", "pay_99");

		assertEquals(ConsultationInvoiceStatus.PAID, paid.getStatus());
		assertEquals("PAID", paid.getPaymentStatus());
		assertEquals("RAZORPAY", paid.getPaymentMode());
		assertEquals("pay_99", paid.getTransactionId());
		assertEquals(0, paid.getBalance().compareTo(BigDecimal.ZERO));
	}

	@Test
	void completeRazorpayCapture_missingInvoice_notFound() {
		when(invoiceRepository.findByRazorpayOrderId("order_missing")).thenReturn(Optional.empty());
		CustomException ex = assertThrows(CustomException.class,
				() -> service.completeRazorpayCapture("order_missing", "pay_1"));
		assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
	}
}
