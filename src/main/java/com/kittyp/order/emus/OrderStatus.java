/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.emus;

/**
 * @author rrohan419@gmail.com 
 */
public enum OrderStatus{
	CREATED,        // When order is created
    SUCCESSFULL,     // When payment is authorized (especially for UPI, Netbanking)
    DELIVERED,       // Final successful DELIVERY state
    FAILED,         // Payment failed
    REFUNDED,       // If refund happens
    CANCELLED,      // If order is manually cancelled
	REFUND_INITIATED,
    CAPTURED,
    PROCESSING,
    PAYMENT_PENDING,
    PAYMENT_TIMEOUT,
    PAYMENT_CANCELLED;
    
    public static OrderStatus fromRazorpayStatus(String status) {
        return switch (status.toUpperCase()) {
            case "CREATED" -> CREATED;
            case "CAPTURED" -> CAPTURED;
            case "PROCESSING" -> PROCESSING;
            case "PAID" -> SUCCESSFULL;
            case "REFUNDED" -> REFUNDED;
            case "CANCELLED" -> CANCELLED;
            case "PAYMENT_PENDING" -> PAYMENT_PENDING;
            case "PAYMENT_TIMEOUT" -> PAYMENT_TIMEOUT;
            case "PAYMENT_CANCELLED" -> PAYMENT_CANCELLED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }
}
