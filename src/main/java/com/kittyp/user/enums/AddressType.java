package com.kittyp.user.enums;

public enum AddressType {
    SHIPPING_ADDRESS,
    BILLING_ADDRESS,
    BOTH;

    public static AddressType fromString(String type) {
        for (AddressType addressType : AddressType.values()) {
            if (addressType.name().equalsIgnoreCase(type)) {
                return addressType;
            }
        }
        throw new IllegalArgumentException("Unknown address type: " + type);
    }
}
