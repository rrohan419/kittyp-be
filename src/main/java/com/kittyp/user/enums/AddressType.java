package com.kittyp.user.enums;

public enum AddressType {
    HOME, OFFICE, OTHERS;

    public static AddressType fromString(String type) {
        for (AddressType addressType : AddressType.values()) {
            if (addressType.name().equalsIgnoreCase(type)) {
                return addressType;
            }
        }
        throw new IllegalArgumentException("Unknown address type: " + type);
    }
}
