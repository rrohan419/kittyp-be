package com.kittyp.clinic.enums;

public enum ClinicStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    SHUTDOWN;

    public static final String NOT_ACTIVATED_MESSAGE =
            "This clinic must be verified by admin before appointments, bookings, or adding doctors.";

    /** Bookable / can invite doctors. Shutdown and pending/rejected are not. */
    public boolean isActivated() {
        return this == VERIFIED;
    }
}
