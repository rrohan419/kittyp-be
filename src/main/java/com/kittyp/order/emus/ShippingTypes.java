/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.emus;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.kittyp.common.exception.CustomException;

public enum ShippingTypes {

    EXPRESS_SHIPPING(BigDecimal.valueOf(150).setScale(2, RoundingMode.UNNECESSARY), "Express (1‑2 days)"),
    STANDARD_SHIPPING(BigDecimal.valueOf(100).setScale(2, RoundingMode.UNNECESSARY), "Standard (3‑5 days)");

    private final BigDecimal cost;
    private final String label;

    ShippingTypes(BigDecimal cost, String label) {
        this.cost = cost;
        this.label = label;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getLabel() {
        return label;
    }

//    /** Case‑insensitive, dash/space tolerant mapper */
//    public static ShippingTypes fromString(String value) {
//        if (value == null || value.isBlank()) {
//            throw new CustomException("Shipping method is required");
//        }
//        String normalised = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
//        try {
//            return ShippingTypes.valueOf(normalised);
//        } catch (IllegalArgumentException e) {
//            throw new CustomException("Invalid shipping method: " + value);
//        }
//    }
}
