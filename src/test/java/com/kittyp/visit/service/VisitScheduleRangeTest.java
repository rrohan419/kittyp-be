package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class VisitScheduleRangeTest {

    @Test
    void normalizeDateRange_swapsWhenInverted() {
        LocalDate[] range = VisitServiceImpl.normalizeDateRange(
                LocalDate.of(2026, 8, 30),
                LocalDate.of(2026, 8, 24));
        assertEquals(LocalDate.of(2026, 8, 24), range[0]);
        assertEquals(LocalDate.of(2026, 8, 30), range[1]);
    }

    @Test
    void normalizeDateRange_usesSingleDayWhenToMissing() {
        LocalDate[] range = VisitServiceImpl.normalizeDateRange(LocalDate.of(2026, 8, 30), null);
        assertEquals(LocalDate.of(2026, 8, 30), range[0]);
        assertEquals(LocalDate.of(2026, 8, 30), range[1]);
    }

    @Test
    void normalizeDateRange_usesToWhenFromMissing() {
        LocalDate day = LocalDate.of(2026, 8, 25);
        LocalDate[] range = VisitServiceImpl.normalizeDateRange(null, day);
        assertEquals(day, range[0]);
        assertEquals(day, range[1]);
    }
}
