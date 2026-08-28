package com.kittyp.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptedStringConverter();
        // Force init via reflection-free path: convert will use fallback until PostConstruct
    }

    @Test
    void roundTripEncryptsAndDecrypts() {
        String plain = "EAAG.test-token-value";
        String stored = converter.convertToDatabaseColumn(plain);
        assertTrue(stored.startsWith("enc:v1:"));
        assertNotEquals(plain, stored);
        assertEquals(plain, converter.convertToEntityAttribute(stored));
    }

    @Test
    void legacyPlaintextStillReadable() {
        assertEquals("legacy-token", converter.convertToEntityAttribute("legacy-token"));
    }

    @Test
    void blankPassthrough() {
        assertEquals(null, converter.convertToDatabaseColumn(null));
        assertEquals("", converter.convertToDatabaseColumn(""));
    }

    @Test
    void doesNotDoubleEncrypt() {
        String once = converter.convertToDatabaseColumn("tok");
        String twice = converter.convertToDatabaseColumn(once);
        assertEquals(once, twice);
        assertFalse(twice.substring(7).contains("enc:v1:"));
    }
}
