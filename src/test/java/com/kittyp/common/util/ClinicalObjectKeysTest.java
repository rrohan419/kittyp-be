package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.kittyp.common.exception.CustomException;

class ClinicalObjectKeysTest {

    @Test
    void key_matchesAuditPattern_withVisitAnchor() {
        String key = ClinicalObjectKeys.key("CL8K2P", "9AP1AU", "labs", YearMonth.of(2026, 8),
                ClinicalObjectKeys.visitAnchor("a1b2c3"), "CBC report.pdf");
        assertEquals("clinical/CL8K2P/9AP1AU/labs/2026/08/visit-a1b2c3_CBC_report.pdf", key);
    }

    @Test
    void key_usesEventAnchor_whenNoVisit() {
        String key = ClinicalObjectKeys.key("CL8K2P", "9AP1AU", "surgeries", YearMonth.of(2026, 8),
                ClinicalObjectKeys.eventAnchor("evt-99"), "note.pdf");
        assertEquals("clinical/CL8K2P/9AP1AU/surgeries/2026/08/event-evt-99_note.pdf", key);
    }

    @Test
    void sanitizeFileName_stripsPathSegmentsAndSpaces() {
        assertEquals("CBC_report.pdf", ClinicalObjectKeys.sanitizeFileName("../labs/CBC report.pdf"));
    }

    @Test
    void normalizeKind_rejectsUnknown() {
        CustomException ex = assertThrows(CustomException.class, () -> ClinicalObjectKeys.normalizeKind("xrays"));
        assertTrue(ex.getMessage().contains("labs"));
    }
}
