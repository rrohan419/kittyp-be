package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dto.OwnerInvoiceModel;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.user.service.PetAccessGuard;

@ExtendWith(MockitoExtension.class)
class TreatmentInvoiceOwnerAccessTest {

	@Mock
	private ConsultationInvoiceRepository invoiceRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PetAccessGuard petAccessGuard;

	@InjectMocks
	private TreatmentInvoiceService service;

	@Test
	void listForPetOwner_returnsInvoicesForOwnedPet() {
		User owner = User.builder().email("priya@example.com").firstName("Priya").build();
		owner.setId(9L);
		ConsultationInvoice invoice = ConsultationInvoice.builder()
				.uuid("inv-1")
				.invoiceNumber("INV-1")
				.petUuid("pet-1")
				.visitUuid("visit-1")
				.amount(new BigDecimal("800.00"))
				.paidAmount(new BigDecimal("800.00"))
				.currency("INR")
				.paymentStatus("PAID")
				.doctorNotes("Continue the prescribed diet.")
				.pdfUrl("invoices/inv-1.pdf")
				.status(ConsultationInvoiceStatus.PAID)
				.owner(owner)
				.build();
		when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(owner));
		when(invoiceRepository.findAllByPetUuidOrderByCreatedAtDesc("pet-1")).thenReturn(List.of(invoice));
		when(invoiceRepository.findAllByOwner_IdOrderByCreatedAtDesc(9L)).thenReturn(List.of(invoice));

		List<OwnerInvoiceModel> rows = service.listForPetOwner("pet-1", "priya@example.com");

		assertEquals(1, rows.size());
		assertEquals("inv-1", rows.get(0).uuid());
		assertEquals("Continue the prescribed diet.", rows.get(0).doctorNotes());
		assertTrue(rows.get(0).pdfAvailable());
		verify(petAccessGuard).requireOwner(owner, "pet-1");
	}

	@Test
	void pdfUrlForPetOwner_rejectsInvoiceForAnotherPet() {
		User owner = User.builder().email("priya@example.com").build();
		owner.setId(9L);
		ConsultationInvoice otherPet = ConsultationInvoice.builder()
				.uuid("inv-2")
				.petUuid("pet-other")
				.build();
		when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(owner));
		when(invoiceRepository.findByUuid("inv-2")).thenReturn(Optional.of(otherPet));

		assertThrows(ResourceNotFoundException.class,
				() -> service.pdfUrlForPetOwner("pet-1", "inv-2", "priya@example.com"));
		verify(petAccessGuard).requireOwner(owner, "pet-1");
	}
}
