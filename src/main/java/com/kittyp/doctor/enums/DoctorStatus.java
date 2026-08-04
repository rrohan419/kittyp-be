package com.kittyp.doctor.enums;

/**
 * Doctor onboarding / verification lifecycle.
 * REGISTERED → DOCUMENTS_SUBMITTED → UNDER_REVIEW → VERIFIED → PUBLISHED
 */
public enum DoctorStatus {
    REGISTERED,
    DOCUMENTS_SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    PUBLISHED,
    REJECTED
}
