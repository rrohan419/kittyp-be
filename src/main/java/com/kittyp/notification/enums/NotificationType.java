package com.kittyp.notification.enums;

public enum NotificationType {
    MISSED_NUTRITION_LOG,
    VACCINATION_DUE,
    DEWORMING_DUE,
    FOOD_REMINDER,
    APPOINTMENT_REMINDER,
    ORDER_STATUS_CHANGE,
    NEW_MESSAGE,
    WEEKLY_DIGEST,
    /** Doctor accepted or declined a clinic invite. */
    CLINIC_DOCTOR_INVITE_RESPONSE,
    /** Clinic assigned / checked in a patient for this doctor. */
    CLINIC_VISIT_ASSIGNED,
    /** Treatment / consultation invoice sent to owner (e.g. WhatsApp). */
    INVOICE_SENT,
    /** Vaccination due reminder (WhatsApp / push / email). */
    VACCINE_REMINDER,
    /** Routine checkup / follow-up reminder. */
    CHECKUP_REMINDER,
    /** Clinic or platform promotional offer. */
    PROMO_OFFER
}
