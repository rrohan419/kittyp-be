package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

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
class TreatmentInvoiceMarkPaidTest {

	@Mock
	private ConsultationInvoiceRepository invoiceRepository;

	@InjectMocks
	private TreatmentInvoiceService service;

	@Test
	void markPaid_setsPaidModeAndTxn() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-1")
				.amount(new BigDecimal("500.00"))
				.paidAmount(BigDecimal.ZERO)
				.balance(new BigDecimal("500.00"))
				.paymentStatus("UNPAID")
				.paymentMode("UPI")
				.status(ConsultationInvoiceStatus.ISSUED)
				.build();
		when(invoiceRepository.save(any(ConsultationInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

		ConsultationInvoice paid = service.markPaid(invoice, "cash", "UPI123");

		assertEquals(ConsultationInvoiceStatus.PAID, paid.getStatus());
		assertEquals("PAID", paid.getPaymentStatus());
		assertEquals("CASH", paid.getPaymentMode());
		assertEquals("UPI123", paid.getTransactionId());
		assertEquals(0, paid.getBalance().compareTo(BigDecimal.ZERO));
		assertEquals(0, paid.getPaidAmount().compareTo(new BigDecimal("500.00")));
	}

	@Test
	void markPaid_alreadyPaid_conflict() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-2")
				.amount(new BigDecimal("100.00"))
				.paidAmount(new BigDecimal("100.00"))
				.balance(BigDecimal.ZERO)
				.paymentStatus("PAID")
				.status(ConsultationInvoiceStatus.PAID)
				.build();

		CustomException ex = assertThrows(CustomException.class,
				() -> service.markPaid(invoice, "CASH", null));
		assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
	}

	@Test
	void normalizeRecordedPaymentMode_rejectsUnknown() {
		CustomException ex = assertThrows(CustomException.class,
				() -> TreatmentInvoiceService.normalizeRecordedPaymentMode("bitcoin"));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
	}
}
