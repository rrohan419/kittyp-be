package com.kittyp.clinic.enums;

public enum ClinicStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    NOT_REQUIRED,
    SHUTDOWN;

    public static final String NOT_ACTIVATED_MESSAGE =
            "This clinic must be verified by admin before appointments, bookings, or adding doctors.";

    /** Bookable / can invite doctors. NOT_REQUIRED is used by personal practices. */
    public boolean isActivated() {
        return this == VERIFIED || this == NOT_REQUIRED;
    }
}
