package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class TreatmentInvoiceWhatsAppPhoneTest {

    @InjectMocks
    private TreatmentInvoiceService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsArbitraryOverridePhone() throws Exception {
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .ownerSnapshot(objectMapper.writeValueAsString(Map.of("ownerPhone", "9876543210")))
                .build();

        CustomException ex = assertThrows(CustomException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveOwnerPhone", invoice, "9123456789"));
        assertEquals("WhatsApp destination must match the invoice owner phone", ex.getMessage());
    }

    @Test
    void allowsSameNumberWithDifferentFormatting() throws Exception {
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .ownerSnapshot(objectMapper.writeValueAsString(Map.of("ownerPhone", "9876543210")))
                .build();

        String phone = ReflectionTestUtils.invokeMethod(
                service, "resolveOwnerPhone", invoice, "+91 98765-43210");
        assertEquals("9876543210", phone);
    }

    @Test
    void usesLinkedOwnerWhenSnapshotMissing() {
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        User owner = new User();
        owner.setPhoneNumber("9988776655");
        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .ownerSnapshot("{}")
                .owner(owner)
                .build();

        String phone = ReflectionTestUtils.invokeMethod(service, "resolveOwnerPhone", invoice, null);
        assertEquals("9988776655", phone);
    }
}
