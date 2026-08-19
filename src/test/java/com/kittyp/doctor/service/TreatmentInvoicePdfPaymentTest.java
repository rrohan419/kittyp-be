package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.doctor.dto.TreatmentInvoiceData;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;

class TreatmentInvoicePdfPaymentTest {

	private final TreatmentInvoiceService service = new TreatmentInvoiceService(
			null, null, null, null, null, null, null, null, null, null,
			new ObjectMapper(), null, null, null, null);

	@Test
	void paidInvoice_showsRazorpayDetails() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-1")
				.invoiceNumber("INV-2026-000018")
				.amount(new BigDecimal("55002.00"))
				.paidAmount(new BigDecimal("55002.00"))
				.balance(BigDecimal.ZERO)
				.paymentStatus("PAID")
				.paymentMode("RAZORPAY")
				.transactionId("pay_99abc")
				.razorpayOrderId("order_rzp_1")
				.status(ConsultationInvoiceStatus.PAID)
				.lineItems("[]")
				.build();
		invoice.setUpdatedAt(LocalDateTime.of(2026, 8, 19, 15, 30));

		TreatmentInvoiceData data = service.toPdfData(invoice);

		assertTrue(data.isPaid());
		assertFalse(data.isPartial());
		assertEquals("PAID", data.getPaymentStatus());
		assertEquals("Paid", data.getPaymentStatusLabel());
		assertEquals("Online (Razorpay)", data.getPaymentModeLabel());
		assertEquals("pay_99abc", data.getTransactionId());
		assertEquals("order_rzp_1", data.getRazorpayOrderId());
		assertEquals("19 Aug 2026", data.getPaidAt());
		assertEquals("This invoice is paid in full. Thank you.", data.getPaymentSummary());
	}

	@Test
	void markPaidCash_showsModeWithoutRazorpayOrder() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-cash")
				.invoiceNumber("INV-2026-000019")
				.amount(new BigDecimal("1200.00"))
				.paidAmount(new BigDecimal("1200.00"))
				.balance(BigDecimal.ZERO)
				.paymentStatus("PAID")
				.paymentMode("CASH")
				.transactionId("CASH-DESK")
				.status(ConsultationInvoiceStatus.PAID)
				.lineItems("[]")
				.build();

		TreatmentInvoiceData data = service.toPdfData(invoice);

		assertTrue(data.isPaid());
		assertEquals("Paid", data.getPaymentStatusLabel());
		assertEquals("Cash", data.getPaymentModeLabel());
		assertEquals("CASH-DESK", data.getTransactionId());
		assertNull(data.getRazorpayOrderId());
	}

	@Test
	void unpaidInvoice_hidesTransactionAndShowsDue() {
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-2")
				.invoiceNumber("INV-2026-000018")
				.amount(new BigDecimal("55002.00"))
				.paidAmount(BigDecimal.ZERO)
				.balance(new BigDecimal("55002.00"))
				.paymentStatus("UNPAID")
				.paymentMode("UPI")
				.status(ConsultationInvoiceStatus.ISSUED)
				.lineItems("[]")
				.build();

		TreatmentInvoiceData data = service.toPdfData(invoice);

		assertFalse(data.isPaid());
		assertEquals("UNPAID", data.getPaymentStatus());
		assertEquals("Unpaid", data.getPaymentStatusLabel());
		assertEquals("UPI", data.getPaymentModeLabel());
		assertNull(data.getTransactionId());
		assertTrue(data.getPaymentSummary().contains("55002.00"));
		assertTrue(data.getPaymentSummary().contains("due"));
	}

	@Test
	void friendlyPatientId_hidesRawUuid() {
		assertNull(TreatmentInvoiceService.friendlyPatientId("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
		assertEquals("MOMO-12", TreatmentInvoiceService.friendlyPatientId("MOMO-12"));
	}

	@Test
	void paymentModeLabel_mapsRazorpay() {
		assertEquals("Online (Razorpay)", TreatmentInvoiceService.paymentModeLabel("RAZORPAY"));
		assertEquals("Cash", TreatmentInvoiceService.paymentModeLabel("Cash"));
	}
}
