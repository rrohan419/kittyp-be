package com.kittyp.payment.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InvoicePdfFileNamerTest {

    @Test
    void fileNameUsesInvoiceNumber() {
        assertEquals("Invoice_INV-2026-000001.pdf",
                InvoicePdfFileNamer.fileName("INV-2026-000001", "A3K9P2"));
    }

    @Test
    void fileNameFallsBackToUuid() {
        assertEquals("Invoice_A3K9P2.pdf", InvoicePdfFileNamer.fileName(null, "A3K9P2"));
    }

    @Test
    void twoInvoiceNumbersProduceTwoNames() {
        String a = InvoicePdfFileNamer.fileName("INV-2026-000001", null);
        String b = InvoicePdfFileNamer.fileName("INV-2026-000002", null);
        assertEquals("Invoice_INV-2026-000001.pdf", a);
        assertEquals("Invoice_INV-2026-000002.pdf", b);
        assertNotEquals(a, b);
    }

    @Test
    void objectKeyPrefixesFolder() {
        assertEquals("treatment-invoices/Invoice_INV-2026-000001.pdf",
                InvoicePdfFileNamer.objectKey("INV-2026-000001", null));
    }

    @Test
    void sanitizeStripsUnsafeCharacters() {
        assertEquals("Invoice_INV-2026-000001-x.pdf",
                InvoicePdfFileNamer.fileName("INV-2026-000001/x", null));
    }

    @Test
    void resolvePrefersStoredKey() {
        assertEquals("treatment-invoices/Invoice_INV-2026-000001.pdf",
                InvoicePdfFileNamer.resolveObjectKey(
                        "treatment-invoices/Invoice_INV-2026-000001.pdf", "legacy-uuid"));
    }

    @Test
    void resolveLegacyUuidKeyWhenPdfUrlBlank() {
        assertEquals("treatment-invoices/abc-def.pdf",
                InvoicePdfFileNamer.resolveObjectKey("  ", "abc-def"));
    }

    @Test
    void resolvePrependsFolderWhenPdfUrlHasNoSlash() {
        assertEquals("treatment-invoices/Invoice_INV-2026-000001.pdf",
                InvoicePdfFileNamer.resolveObjectKey("Invoice_INV-2026-000001.pdf", null));
    }

    @Test
    void baseNameStripsFolderPrefix() {
        assertEquals("Invoice_INV-2026-000001.pdf",
                InvoicePdfFileNamer.baseName("treatment-invoices/Invoice_INV-2026-000001.pdf"));
    }

    @Test
    void fileNameRejectsBlankIdentity() {
        assertThrows(IllegalArgumentException.class, () -> InvoicePdfFileNamer.fileName(" ", null));
    }
}
