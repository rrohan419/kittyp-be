/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.emus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ShippingTypes {

    EXPRESS(BigDecimal.valueOf(199).setScale(2, RoundingMode.UNNECESSARY), "Express (2-3 days)"),
    STANDARD(BigDecimal.valueOf(99).setScale(2, RoundingMode.UNNECESSARY), "Standard (5-7 days)"),
    PRIORITY(BigDecimal.valueOf(499).setScale(2, RoundingMode.UNNECESSARY), "Standard (1-2 days)");
	
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
