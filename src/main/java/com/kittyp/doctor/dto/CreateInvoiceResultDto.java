package com.kittyp.doctor.dto;

import com.kittyp.doctor.entity.ConsultationInvoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/send invoice response: invoice is always persisted; WhatsApp and email are best-effort.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceResultDto {
	private ConsultationInvoice invoice;
	private boolean whatsappSent;
	private String whatsappError;
	private boolean emailSent;
	private String emailError;

	public static CreateInvoiceResultDto of(ConsultationInvoice invoice) {
		return CreateInvoiceResultDto.builder()
				.invoice(invoice)
				.build();
	}

	public static CreateInvoiceResultDto channels(ConsultationInvoice invoice, boolean whatsappSent, String whatsappError,
			boolean emailSent, String emailError) {
		return CreateInvoiceResultDto.builder()
				.invoice(invoice)
				.whatsappSent(whatsappSent)
				.whatsappError(whatsappError)
				.emailSent(emailSent)
				.emailError(emailError)
				.build();
	}

	public static CreateInvoiceResultDto sent(ConsultationInvoice invoice) {
		return channels(invoice, true, null, false, null);
	}

	public static CreateInvoiceResultDto sendFailed(ConsultationInvoice invoice, String error) {
		return channels(invoice, false, error, false, null);
	}
}
