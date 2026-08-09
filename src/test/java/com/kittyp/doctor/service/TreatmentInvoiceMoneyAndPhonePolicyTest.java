package com.kittyp.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dto.CreateConsultationInvoiceDto;
import com.kittyp.doctor.dto.TreatmentLineItemDto;
import com.kittyp.doctor.enums.TreatmentInvoiceItemType;
import com.kittyp.user.entity.User;

@ExtendWith(MockitoExtension.class)
class TreatmentInvoiceMoneyAndPhonePolicyTest {

    @InjectMocks
    private TreatmentInvoiceService service;

    @Test
    void lineTotalIgnoresClientTotal() {
        TreatmentLineItemDto item = new TreatmentLineItemDto();
        item.setItemType(TreatmentInvoiceItemType.CONSULTATION);
        item.setDescription("Consult");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setDiscount(new BigDecimal("10.00"));
        item.setTotal(new BigDecimal("9999.00")); // malicious client total

        BigDecimal total = ReflectionTestUtils.invokeMethod(service, "lineTotal", item);
        assertEquals(new BigDecimal("190.00"), total);
        assertEquals(new BigDecimal("190.00"), item.getTotal());
    }

    @Test
    void snapshotPhoneRejectsMismatchWithLinkedOwner() {
        User owner = new User();
        owner.setPhoneNumber("9876543210");
        CreateConsultationInvoiceDto request = new CreateConsultationInvoiceDto();
        request.setOwnerPhone("9123456789");

        assertThrows(CustomException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveSnapshotPhone", request, owner));
    }

    @Test
    void snapshotPhoneUsesLinkedOwnerWhenRequestBlank() {
        User owner = new User();
        owner.setPhoneNumber("9876543210");
        CreateConsultationInvoiceDto request = new CreateConsultationInvoiceDto();

        String phone = ReflectionTestUtils.invokeMethod(service, "resolveSnapshotPhone", request, owner);
        assertEquals("9876543210", phone);
    }
}
