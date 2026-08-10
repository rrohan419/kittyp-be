package com.kittyp.doctor.dto;

import com.kittyp.doctor.entity.ConsultationInvoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create invoice response: invoice is always persisted; WhatsApp send is best-effort.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceResultDto {
	private ConsultationInvoice invoice;
	private boolean whatsappSent;
	private String whatsappError;

	public static CreateInvoiceResultDto of(ConsultationInvoice invoice) {
		return CreateInvoiceResultDto.builder()
				.invoice(invoice)
				.whatsappSent(false)
				.whatsappError(null)
				.build();
	}

	public static CreateInvoiceResultDto sent(ConsultationInvoice invoice) {
		return CreateInvoiceResultDto.builder()
				.invoice(invoice)
				.whatsappSent(true)
				.whatsappError(null)
				.build();
	}

	public static CreateInvoiceResultDto sendFailed(ConsultationInvoice invoice, String error) {
		return CreateInvoiceResultDto.builder()
				.invoice(invoice)
				.whatsappSent(false)
				.whatsappError(error)
				.build();
	}
}
