package com.kittyp.payment.util;

/**
 * Treatment-invoice PDF export names: {@code Invoice_INV-yyyy-000001.pdf}.
 * Uniqueness is the invoice number (Amazon-style), not generation time.
 */
public final class InvoicePdfFileNamer {

    static final String FOLDER = "treatment-invoices/";

    private InvoicePdfFileNamer() {
    }

    public static String fileName(String invoiceNumber, String uuid) {
        String id = firstNonBlank(invoiceNumber, uuid);
        if (id == null) {
            throw new IllegalArgumentException("invoice identity required");
        }
        return "Invoice_" + sanitize(id) + ".pdf";
    }

    public static String objectKey(String invoiceNumber, String uuid) {
        return FOLDER + fileName(invoiceNumber, uuid);
    }

    /**
     * Prefer stored {@code pdf_url}. Fall back to the legacy
     * {@code treatment-invoices/{uuid}.pdf} key used before named exports.
     */
    public static String resolveObjectKey(String pdfUrl, String invoiceUuid) {
        if (pdfUrl != null && !pdfUrl.isBlank()) {
            String trimmed = pdfUrl.trim();
            return trimmed.contains("/") ? trimmed : FOLDER + trimmed;
        }
        if (invoiceUuid != null && !invoiceUuid.isBlank()) {
            return FOLDER + invoiceUuid.trim() + ".pdf";
        }
        throw new IllegalArgumentException("objectKey required");
    }

    public static String baseName(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey required");
        }
        int slash = objectKey.lastIndexOf('/');
        return slash >= 0 ? objectKey.substring(slash + 1) : objectKey;
    }

    private static String sanitize(String raw) {
        return raw.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }
}
