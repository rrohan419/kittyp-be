package com.kittyp.clinic.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal GS1 Application Identifier parser for barcode wedge input.
 * Never invents missing fields — only returns AIs present in the payload.
 */
public final class Gs1BarcodeParser {

    private static final Pattern AI_PATTERN = Pattern.compile("\\((\\d{2,4})\\)([^\\(]*)");
    private static final DateTimeFormatter EXP_YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private Gs1BarcodeParser() {
    }

    public record ParsedBarcode(
            String raw,
            String gtin,
            String lot,
            LocalDate expiry,
            String serial,
            boolean gs1Structured) {
    }

    public static ParsedBarcode parse(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return new ParsedBarcode("", null, null, null, null, false);
        }
        String raw = rawCode.trim();
        if (raw.contains("(")) {
            Map<String, String> ais = new HashMap<>();
            Matcher m = AI_PATTERN.matcher(raw);
            while (m.find()) {
                ais.put(m.group(1), m.group(2).trim());
            }
            if (!ais.isEmpty()) {
                return new ParsedBarcode(
                        raw,
                        first(ais, "01"),
                        first(ais, "10"),
                        parseExpiry(first(ais, "17")),
                        first(ais, "21"),
                        true);
            }
        }
        // Plain EAN/UPC / GTIN digit string
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 8 && digits.length() <= 14) {
            return new ParsedBarcode(raw, digits, null, null, null, false);
        }
        return new ParsedBarcode(raw, null, null, null, null, false);
    }

    private static String first(Map<String, String> ais, String ai) {
        String v = ais.get(ai);
        return v == null || v.isBlank() ? null : v;
    }

    private static LocalDate parseExpiry(String yyMMdd) {
        if (yyMMdd == null || yyMMdd.length() != 6) {
            return null;
        }
        try {
            // GS1 day 00 means end of month — treat as first of next month minus 1 day via day 01 fallback
            String day = yyMMdd.substring(4, 6);
            if ("00".equals(day)) {
                LocalDate first = LocalDate.parse(yyMMdd.substring(0, 4) + "01", EXP_YYMMDD);
                return first.withDayOfMonth(first.lengthOfMonth());
            }
            return LocalDate.parse(yyMMdd, EXP_YYMMDD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
