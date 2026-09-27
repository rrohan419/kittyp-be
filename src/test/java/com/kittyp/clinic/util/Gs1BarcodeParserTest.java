package com.kittyp.clinic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.kittyp.clinic.util.Gs1BarcodeParser.ParsedBarcode;

class Gs1BarcodeParserTest {

    @Test
    void parsesPlainGtin() {
        ParsedBarcode p = Gs1BarcodeParser.parse("8901234567890");
        assertEquals("8901234567890", p.gtin());
        assertNull(p.lot());
        assertNull(p.expiry());
        assertFalse(p.gs1Structured());
    }

    @Test
    void parsesGs1WithLotAndExpiry() {
        ParsedBarcode p = Gs1BarcodeParser.parse("(01)08901234567890(17)271231(10)BATCH99");
        assertTrue(p.gs1Structured());
        assertEquals("08901234567890", p.gtin());
        assertEquals("BATCH99", p.lot());
        assertEquals(LocalDate.of(2027, 12, 31), p.expiry());
    }

    @Test
    void doesNotInventMissingFields() {
        ParsedBarcode p = Gs1BarcodeParser.parse("(01)12345678");
        assertEquals("12345678", p.gtin());
        assertNull(p.lot());
        assertNull(p.expiry());
        assertNull(p.serial());
    }
}
