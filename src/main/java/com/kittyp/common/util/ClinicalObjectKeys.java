package com.kittyp.common.util;

import java.time.YearMonth;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

/**
 * Audit-friendly S3 keys for clinic pet files in the user bucket:
 * {@code clinical/{clinicUuid}/{petUuid}/{kind}/{yyyy}/{MM}/{anchor}_{file}}.
 */
public final class ClinicalObjectKeys {

    public static final Set<String> KINDS = Set.of("labs", "vaccines", "surgeries");

    private ClinicalObjectKeys() {
    }

    public static String normalizeKind(String kind) {
        String normalized = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
        if (!KINDS.contains(normalized)) {
            throw new CustomException("kind must be labs, vaccines, or surgeries", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    public static String folder(String clinicUuid, String petUuid, String kind, YearMonth month) {
        YearMonth ym = month == null ? YearMonth.now() : month;
        return "clinical/" + safePublicId(clinicUuid) + "/" + safePublicId(petUuid) + "/" + normalizeKind(kind) + "/"
                + ym.getYear() + "/" + String.format("%02d", ym.getMonthValue());
    }

    public static String fileName(String anchor, String originalName) {
        return anchor + "_" + sanitizeFileName(originalName);
    }

    public static String key(String clinicUuid, String petUuid, String kind, YearMonth month, String anchor,
            String originalName) {
        return folder(clinicUuid, petUuid, kind, month) + "/" + fileName(anchor, originalName);
    }

    public static String visitAnchor(String visitUuid) {
        return "visit-" + safePublicId(visitUuid);
    }

    public static String eventAnchor(String eventUuid) {
        return "event-" + safePublicId(eventUuid);
    }

    public static String sanitizeFileName(String originalName) {
        String base = originalName == null || originalName.isBlank() ? "file.bin" : originalName.trim();
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        String cleaned = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) {
            return "file.bin";
        }
        return cleaned;
    }

    public static String safePublicId(String id) {
        if (id == null || id.isBlank()) {
            throw new CustomException("A clinic, pet, or visit id is required", HttpStatus.BAD_REQUEST);
        }
        String cleaned = id.trim().replaceAll("[^a-zA-Z0-9._-]", "");
        if (cleaned.isBlank()) {
            throw new CustomException("Invalid clinic, pet, or visit id", HttpStatus.BAD_REQUEST);
        }
        return cleaned;
    }
}
